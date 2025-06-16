package main

import (
	"bufio"
	"bytes"
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"os/exec"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/feature/s3/manager"
	"github.com/ianlopshire/go-fixedwidth"
	"github.com/stretchr/testify/assert"
)

func TestHandler(t *testing.T) {
	tests := []struct {
		event  events.SQSEvent
		expect string
		err    error
	}{
		{
			event:  getSQSEvent("demo-bucket", "file_path"),
			expect: "file_path",
			err:    nil,
		},
	}

	for _, test := range tests {
		response, err := handler(context.Background(), test.event)
		assert.NotNil(t, err)
		assert.Equal(t, test.expect, response)
	}
}

func TestIntegrationImportResponseFile(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test.")
	}
	today := time.Now().Format("20060102")

	db, _ := createConnectionVar(os.Getenv("DB_USER_DPC_CONSENT"), os.Getenv("DB_PASS_DPC_CONSENT"))
	defer db.Close()

	ctx := context.TODO()
	// clear consent
	deleteConsent := fmt.Sprintf("DELETE FROM consent WHERE effective_date = '%s'", today)
	deleteOptOutFile := fmt.Sprintf("DELETE FROM opt_out_file WHERE created_at > '%s'", today)
	_, consentErr := db.Exec(deleteConsent)
	assert.Nil(t, consentErr)
	_, oofErr := db.Exec(deleteOptOutFile)
	assert.Nil(t, oofErr)

	createdOptOutCount, createdOptInCount, confirmationFileName, err := importResponseFile(ctx, "demo-bucket", "bfdeft01/dpc/in/T.NGD.DPC.RSP.D240123.T1122001.IN")
	assert.Nil(t, err)
	assert.Equal(t, 6, createdOptOutCount)
	assert.Equal(t, 1, createdOptInCount)
	assert.True(t, strings.Contains(confirmationFileName, "T#EFT.ON.DPC.NGD.CONF."))

	// Test confirmation file correct
	b, downloadErr := downloadS3File(ctx, "demo-bucket", confirmationFileName)
	assert.Nil(t, downloadErr)
	r := bytes.NewReader(b)
	scanner := bufio.NewScanner(r)
	ctr := 0
	for scanner.Scan() {
		ctr += 1
		if ctr == 1 {
			var row FileHeader
			fixedwidth.Unmarshal(scanner.Bytes(), &row)
			assert.Equal(t, "HDR_BENECONFIRM", row.HeaderCode)
			assert.Equal(t, today, row.FileCreationDate)
		} else if ctr < 9 {
			var row ConfirmationFileRow
			fixedwidth.Unmarshal(scanner.Bytes(), &row)
			assert.Equal(t, fmt.Sprintf("%dSJ0A00AA00", ctr - 1), row.MBI)
			assert.Equal(t, today, row.EffectiveDate)
			assert.Equal(t, "Accepted", row.RecordStatus)
			assert.Equal(t, "00", row.ReasonCode)
			if ctr == 7 {
				assert.Equal(t, "Y", row.SharingPreference)
			} else {
				assert.Equal(t, "N", row.SharingPreference)
			}
		} else {
			var row FileTrailer
			fixedwidth.Unmarshal(scanner.Bytes(), &row)
			assert.Equal(t, "TRL_BENECONFIRM", row.TrailerCode)
			assert.Equal(t, today, row.FileCreationDate)
			assert.Equal(t, "0000000007", row.DetailRecordCount)
		}
	}

	// test database updated
	countOptOutFile := fmt.Sprintf("SELECT COUNT(*) FROM opt_out_file WHERE import_status = 'Completed' AND created_at > '%s'", today)
	var oofCount int
	cOofErr := db.QueryRow(countOptOutFile).Scan(&oofCount)
	assert.Nil(t, cOofErr)
	assert.Equal(t, 1, oofCount)

	// Fetch opt_out_file id for checking consent inserts
	queryOptOutFile := fmt.Sprintf("SELECT id FROM opt_out_file WHERE import_status = 'Completed' AND created_at > '%s'", today)
	var oofId string
	qOofErr := db.QueryRow(queryOptOutFile).Scan(&oofId)
	assert.Nil(t, qOofErr)
	assert.NotNil(t, oofId)

	countConsent := fmt.Sprintf("SELECT COUNT(*) FROM consent WHERE effective_date = '%s'", today)
	var consentCount int
	qConsentErr := db.QueryRow(countConsent).Scan(&consentCount)
	assert.Nil(t, qConsentErr)
	assert.Equal(t, 7, consentCount)

	queryConsent := fmt.Sprintf("SELECT policy_code, COUNT(*) FROM consent WHERE opt_out_file_id = '%s' AND effective_date = '%s' GROUP BY policy_code", oofId, today)
	var pc string
	var num int
	consentRows, consentErr := db.Query(queryConsent)
	defer consentRows.Close()
	assert.Nil(t, consentErr)
	for consentRows.Next() {
		consentRows.Scan(&pc, &num)
		if pc == "OPTOUT" {
			assert.Equal(t, 6, num)
		} else if pc == "OPTIN" {
			assert.Equal(t, 1, num)
		}
	}
}

func TestHandlerDatabaseTimeoutError(t *testing.T) {
	//test timeout error is propragated to lambda
	ofn := createConnectionVar
	createConnectionVar = func(string, string) (*sql.DB, error) { return nil, errors.New("Connection attempt timed out") }
	defer func() { createConnectionVar = ofn }()

	event := getSQSEvent("demo-bucket", "T.NGD.DPC.RSP.D240123.T1122001.IN")
	_, err := handler(context.Background(), event)

	assert.EqualError(t, err, "Connection attempt timed out")
}

func TestIntegrationDownloadS3File(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test.")
	}

	loadS3()
	f, err := os.ReadFile("dummyfile.txt")
	if err != nil {
		fmt.Printf("unable to read file: %v", err)
	}
	tests := []struct {
		name         string
		bucket       string
		filenamePath string
		expect       string
		err          error
	}{
		{
			name:         "happy path",
			bucket:       "demo-bucket",
			filenamePath: "dummyfile.txt",
			expect:       string(f),
			err:          nil,
		},
		{
			name:         "non-existent file error",
			bucket:       "demo-bucket",
			filenamePath: "nonexistentfile.txt",
			expect:       "",
			err:          errors.New("NoSuchKey"),
		},
	}

	for _, test := range tests {
		fmt.Printf("~~~ %s test\n", test.name)
		response, err := downloadS3File(context.TODO(), test.bucket, test.filenamePath)
		assert.Equal(t, test.expect, string(response[:]))
		if test.err != nil {
			assert.ErrorContains(t, err, test.err.Error())
		}
	}

}

func TestGenerateConfirmationFile(t *testing.T) {
	output1 := fmt.Sprintf(`HDR_BENECONFIRM%s
1SJ0A00AA0020240110NAccepted  00
2SJ0A00AA0020240110YAccepted  00
TRL_BENECONFIRM%s0000000002`, time.Now().Format("20060102"), time.Now().Format("20060102"))
	output2 := fmt.Sprintf(`HDR_BENECONFIRM%s
1SJ0A00AA0020240110NRejected  02
TRL_BENECONFIRM%s0000000001`, time.Now().Format("20060102"), time.Now().Format("20060102"))
	tests := []struct {
		name       string
		successful bool
		records    []*OptOutRecord
		marshaller FileMarshaler
		expected   string
	}{
		{
			name:       "successful-import",
			successful: true,
			records: []*OptOutRecord{
				{
					ID:           "test1",
					OptOutFileID: "2",
					MBI:          "1SJ0A00AA00",
					PolicyCode:   "OPTOUT",
					EffectiveDt:  time.Date(2024, 01, 10, 0, 0, 0, 0, time.UTC),
					Status:       Accepted,
				},
				{
					ID:           "test2",
					OptOutFileID: "2",
					MBI:          "2SJ0A00AA00",
					PolicyCode:   "OPTIN",
					EffectiveDt:  time.Date(2024, 01, 10, 0, 0, 0, 0, time.UTC),
					Status:       Accepted,
				},
			},
			marshaller: fixedwidth.Marshal,
			expected:   output1,
		},
		{
			name:       "unsuccessful-import",
			successful: false,
			records: []*OptOutRecord{
				{
					ID:           "test1",
					OptOutFileID: "2",
					MBI:          "1SJ0A00AA00",
					PolicyCode:   "OPTOUT",
					EffectiveDt:  time.Date(2024, 01, 10, 0, 0, 0, 0, time.UTC),
					Status:       Rejected,
				},
			},
			marshaller: fixedwidth.Marshal,
			expected:   output2,
		},
	}

	for _, test := range tests {
		fmt.Printf("~~~ %s test\n", test.name)
		output, err := generateConfirmationFile(test.successful, test.records, test.marshaller)
		assert.Equal(t, test.expected, string(output[:]))
		assert.Equal(t, err, nil)
	}
}

func TestUploadConfirmationFile(t *testing.T) {
	tests := []struct {
		name             string
		bucket           string
		file             string
		err              error
		confirmationFile []byte
		uploader         S3Uploader
	}{
		{
			name:             "happy-path",
			bucket:           "demo-bucket",
			file:             "fake-file",
			err:              nil,
			confirmationFile: []byte("test"),
			//ploader func(ctx context.Context, input *s3.PutObjectInput, opts ...func(*manager.Uploader)) (*manager.UploadOutput, error)
			uploader: func(ctx context.Context, input *s3.PutObjectInput, opts ...func(*manager.Uploader)) (*manager.UploadOutput, error) {
				return nil, nil
			},
		},
		{
			name:             "upload_fails",
			bucket:           "demo-bucket",
			file:             "fake-file",
			err:              errors.New("upload failed"),
			confirmationFile: []byte("test"),
			uploader: func(ctx context.Context, input *s3.PutObjectInput, opts ...func(*manager.Uploader)) (*manager.UploadOutput, error) {
				return nil, errors.New("upload failed")
			},
		},
	}

	for _, test := range tests {
		fmt.Printf("~~~ %s test\n", test.name)
		err := uploadConfirmationFile(context.TODO(), test.bucket, test.file, test.uploader, test.confirmationFile)
		assert.Equal(t, test.err, err)
	}

}

func TestIntegrationDeleteS3File(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test.")
	}
	loadS3()
	tests := []struct {
		name         string
		bucket       string
		filenamePath string
		err          error
	}{
		{
			name:         "happy path",
			bucket:       "demo-bucket",
			filenamePath: "dummyfile.txt",
			err:          nil,
		},
	}

	for _, test := range tests {
		fmt.Printf("~~~ %s test\n", test.name)
		err := deleteS3File(context.TODO(), test.bucket, test.filenamePath)
		if test.err == nil {
			assert.NoError(t, err)
		} else {
			assert.ErrorContains(t, err, test.err.Error())
		}
	}
}

func getSQSEvent(bucketName string, fileName string) events.SQSEvent {
	jsonFile, err := os.Open("testdata/s3event.json")
	if err != nil {
		fmt.Println(err)
	}
	defer jsonFile.Close()

	byteValue, _ := io.ReadAll(jsonFile)
	if err != nil {
		fmt.Println(err)
	}

	var s3event events.S3Event
	err = json.Unmarshal([]byte(byteValue), &s3event)
	if err != nil {
		fmt.Println(err)
	}

	s3event.Records[0].S3.Bucket.Name = bucketName
	s3event.Records[0].S3.Object.Key = fileName

	val, err := json.Marshal(s3event)

	if err != nil {
		fmt.Println(err)
	}

	body := fmt.Sprintf("{\"Type\" : \"Notification\",\n  \"MessageId\" : \"123456-1234-1234-1234-6e06896db643\",\n  \"TopicArn\" : \"my-topic\",\n  \"Subject\" : \"Amazon S3 Notification\",\n  \"Message\" : %s}", strconv.Quote(string(val[:])))
	event := events.SQSEvent{
		Records: []events.SQSMessage{{Body: body}},
	}
	return event
}

func loadS3() {
	cmd := exec.Command("./populate_s3.sh")
	cmd.Run()
}
