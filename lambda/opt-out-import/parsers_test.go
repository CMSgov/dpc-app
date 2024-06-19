package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"os"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/aws/aws-lambda-go/events"
	"github.com/ianlopshire/go-fixedwidth"
	giterr "github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
)

func TestParseMetadata(t *testing.T) {

	// positive
	expTime, _ := time.Parse(time.RFC3339, "2024-01-23T11:22:00Z")
	metadata, _ := ParseMetadata("blah", "T.NGD.DPC.RSP.D240123.T1122001.IN")
	assert.Equal(t, "T.NGD.DPC.RSP.D240123.T1122001.IN", metadata.Name)
	assert.Equal(t, expTime.Format("D060102.T150405"), metadata.Timestamp.Format("D060102.T150405"))

	// change the name and timestamp
	expTime, _ = time.Parse(time.RFC3339, "2019-01-23T11:22:00Z")
	metadata, _ = ParseMetadata("blah", "T.NGD.DPC.RSP.D190123.T1122001.IN")
	assert.Equal(t, "T.NGD.DPC.RSP.D190123.T1122001.IN", metadata.Name)
	assert.Equal(t, expTime.Format("D060102.T150405"), metadata.Timestamp.Format("D060102.T150405"))

	// prod environment with correct prefix
	testEnv := os.Getenv("ENV")

	os.Setenv("ENV", "prod")
	defer os.Setenv("ENV", testEnv)

	expTime, _ = time.Parse(time.RFC3339, "2019-01-23T11:22:00Z")
	metadata, _ = ParseMetadata("blah", "P.NGD.DPC.RSP.D190123.T1122001.IN")
	assert.Equal(t, "P.NGD.DPC.RSP.D190123.T1122001.IN", metadata.Name)
	assert.Equal(t, expTime.Format("D060102.T150405"), metadata.Timestamp.Format("D060102.T150405"))
}

func TestParseMetadata_InvalidData(t *testing.T) {

	// invalid file name
	_, err := ParseMetadata("path", "file")
	assert.EqualError(t, err, "invalid filename for file: file")

	_, err = ParseMetadata("/path", "T.NGD.DPC.RSP.D240123.T1122001")
	assert.EqualError(t, err, "invalid filename for file: T.NGD.DPC.RSP.D240123.T1122001")

	// invalid date
	_, err = ParseMetadata("/path", "T.NGD.DPC.RSP.D190117.T9909420.IN")
	assert.EqualError(t, err, "failed to parse date 'D190117.T990942' from file: T.NGD.DPC.RSP.D190117.T9909420.IN: parsing time \"D190117.T990942\": hour out of range")

	// invalid prefix for test environment
	_, err = ParseMetadata("/path", "P.NGD.DPC.RSP.D190123.T1122001.IN")
	assert.EqualError(t, err, "invalid filename for file: P.NGD.DPC.RSP.D190123.T1122001.IN")
}

func TestParseRecord(t *testing.T) {
	// 181120 file
	fileTime, _ := time.Parse(time.RFC3339, "2018-11-20T10:00:00Z")
	line := []byte("1SJ0A00AA00N")
	metadata := &ResponseFileMetadata{
		Timestamp:    fileTime,
		FilePath:     "full-fake-filename",
		Name:         "fake-filename",
		DeliveryDate: time.Now(),
	}

	tests := []struct {
		number      int
		fileTime    time.Time
		line        []byte
		metadata    *ResponseFileMetadata
		unmarshaler FileUnmarshaler
		err         error
	}{
		{
			number:      1,
			fileTime:    fileTime,
			line:        []byte("1SJ0A00AA00N"),
			metadata:    metadata,
			unmarshaler: fixedwidth.Unmarshal,
			err:         nil,
		},
		{
			number:      2,
			fileTime:    fileTime,
			line:        []byte("1SJ0A00AA00Y"),
			metadata:    metadata,
			unmarshaler: fixedwidth.Unmarshal,
			err:         nil,
		},
		{
			number:   3,
			fileTime: fileTime,
			line:     line,
			metadata: metadata,
			unmarshaler: func(data []byte, v interface{}) error {
				return errors.New("Unmarshaling failed")
			},
			err: errors.New("Unmarshaling failed"),
		},
	}

	for _, test := range tests {
		suppression, err := ParseRecord(test.metadata, test.line, test.unmarshaler)
		if test.number == 1 {
			assert.Equal(t, "1SJ0A00AA00", suppression.MBI)
			assert.Equal(t, "OPTOUT", suppression.PolicyCode)
		} else if test.number == 2 {
			assert.Equal(t, "1SJ0A00AA00", suppression.MBI)
			assert.Equal(t, "OPTIN", suppression.PolicyCode)
		} else {
			assert.Equal(t, test.err, giterr.Cause(err))
		}
	}
}

func TestParseRecord_InvalidData(t *testing.T) {
	fp := "testfilepath"
	fn := "testfilename"

	tests := []struct {
		line   string
		expErr string
	}{
		{
			"1SJ0A00AA00Q",
			"failed to parse file: testfilepath: Unexpected value Q for sharing preference",
		},
	}

	for _, tt := range tests {
		t.Run(tt.line, func(t *testing.T) {
			metadata := &ResponseFileMetadata{
				Timestamp:    time.Now(),
				FilePath:     fp,
				Name:         fn,
				DeliveryDate: time.Now(),
			}
			suppression, err := ParseRecord(metadata, []byte(tt.line), fixedwidth.Unmarshal)
			assert.Nil(t, suppression)
			assert.NotNil(t, err)
			assert.Contains(t, err.Error(), tt.expErr)
		})
	}
}

func TestParseRecord_FailOnRealMBINotProd(t *testing.T) {
	fp := "testfilepath"
	fn := "testfilename"
	lines := [2]string{"1EG4TE5MK73Y", "1eg4te5mk73Y"} // n.b. Although it matches the pattern, this is not a real MBI
	
	metadata := &ResponseFileMetadata{
		Timestamp:    time.Now(),
		FilePath:     fp,
		Name:         fn,
		DeliveryDate: time.Now(),
	}

	expErr := "failed to parse file: testfilepath: Valid MBI in non-production environment"
	for _, line := range lines {
		suppression, err := ParseRecord(metadata, []byte(line), fixedwidth.Unmarshal)
		assert.Nil(t, suppression)
		assert.NotNil(t, err)
		assert.Contains(t, err.Error(), expErr)
	}

	// Should pass on prod
	testEnv := os.Getenv("ENV")
	os.Setenv("ENV", "prod")
	defer os.Setenv("ENV", testEnv)

	for _, line := range lines {
		suppression, err := ParseRecord(metadata, []byte(line), fixedwidth.Unmarshal)
		assert.Nil(t, err)
		assert.Equal(t, "1EG4TE5MK73", strings.ToUpper(suppression.MBI))
		assert.Equal(t, "OPTIN", suppression.PolicyCode)
	}
}

func TestParseSQSEvent(t *testing.T) {
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

	val, err := json.Marshal(s3event)

	if err != nil {
		fmt.Println(err)
	}

	body := fmt.Sprintf("{\"Type\" : \"Notification\",\n  \"MessageId\" : \"123456-1234-1234-1234-6e06896db643\",\n  \"TopicArn\" : \"my-topic\",\n  \"Subject\" : \"Amazon S3 Notification\",\n  \"Message\" : %s}", strconv.Quote(string(val[:])))

	event := events.SQSEvent{
		Records: []events.SQSMessage{{Body: body}},
	}

	s3Event, err := ParseSQSEvent(event)
	assert.Nil(t, err)
	assert.NotNil(t, s3Event)
	assert.Equal(t, "demo-bucket", s3Event.Records[0].S3.Bucket.Name)
}
