package main

import (
	"context"
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"os"
	"os/exec"
	"testing"
	"time"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-sdk-go/service/s3/s3manager"
	"github.com/ianlopshire/go-fixedwidth"
	"github.com/stretchr/testify/assert"
)

func TestHandler(t *testing.T) {

	tests := []struct {
		event  events.S3Event
		expect string
		err    error
	}{
		{
			event:  getS3Event("demo-bucket", "file_path"),
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

func TestHandlerDatabaseTimeoutError(t *testing.T) {
	//test timeout error is propragated to lambda

	ofn := createConnectionVar
	createConnectionVar = func(string, string) (*sql.DB, error) { return nil, errors.New("Connection attempt timed out") }
	defer func() { createConnectionVar = ofn }()

	event := getS3Event("demo-bucket", "P.NGD.DPC.RSP.D240123.T1122001.IN")
	_, err := handler(context.Background(), event)

	assert.EqualError(t, err, "Connection attempt timed out")
}

func TestDownloadS3File(t *testing.T) {
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
		response, err := downloadS3File(test.bucket, test.filenamePath)
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
TLR_BENECONFIRM%s0000000002`, time.Now().Format("20060102"), time.Now().Format("20060102"))
	output2 := fmt.Sprintf(`HDR_BENECONFIRM%s
1SJ0A00AA0020240110NRejected  02
TLR_BENECONFIRM%s0000000001`, time.Now().Format("20060102"), time.Now().Format("20060102"))
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
			uploader: func(input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) {
				return nil, nil
			},
		},
		{
			name:             "upload_fails",
			bucket:           "demo-bucket",
			file:             "fake-file",
			err:              errors.New("upload failed"),
			confirmationFile: []byte("test"),
			uploader: func(input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) {
				return nil, errors.New("upload failed")
			},
		},
	}

	for _, test := range tests {
		fmt.Printf("~~~ %s test\n", test.name)
		err := uploadConfirmationFile(test.bucket, test.file, test.uploader, test.confirmationFile)
		assert.Equal(t, test.err, err)
	}

}

func TestDeleteS3File(t *testing.T) {
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
		err := deleteS3File(test.bucket, test.filenamePath)
		if test.err != nil {
			assert.ErrorContains(t, err, test.err.Error())
		}
	}
}

func getS3Event(bucketName string, fileName string) events.S3Event {
	var s3event events.S3Event

	jsonFile, err := os.Open("testdata/s3event.json")
	if err != nil {
		fmt.Println(err)
	}
	defer jsonFile.Close()

	byteValue, _ := ioutil.ReadAll(jsonFile)
	if err != nil {
		fmt.Println(err)
	}

	err = json.Unmarshal([]byte(byteValue), &s3event)
	if err != nil {
		fmt.Println(err)
	}
	s3event.Records[0].S3.Bucket.Name = bucketName
	s3event.Records[0].S3.Object.Key = fileName
	return s3event
}

func loadS3() {
	cmd := exec.Command("./populate_s3.sh")
	cmd.Run()
}
