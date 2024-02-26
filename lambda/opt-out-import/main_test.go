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

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-sdk-go/service/s3/s3manager"
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

	event := getS3Event("demo-bucket", "T#EFT.ON.ACO.NGD1800.DPRF.D181120.T1000009")
	_, err := handler(context.Background(), event)

	assert.EqualError(t, err, "Connection attempt timed out")
}

func TestDownloadS3File(t *testing.T) {
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

func TestUploadResponseFile(t *testing.T) {
	tests := []struct {
		name      string
		bucket    string
		file      string
		err       error
		record    []*OptOutRecord
		uploader  S3Uploader
		marshaler FileMarshaler
	}{
		{
			name:   "happy-path",
			bucket: "demo-bucket",
			file:   "fake-file",
			err:    nil,
			record: []*OptOutRecord{
				{
					ID:           "Id",
					OptOutFileID: "FileId",
					MBI:          "Mbi",
					Status:       Accepted,
				},
			},
			uploader: func(input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) {
				return nil, nil
			},
			marshaler: func(v interface{}) ([]byte, error) {
				return nil, nil
			},
		},
		{
			name:   "upload_fails",
			bucket: "demo-bucket",
			file:   "fake-file",
			err:    errors.New("upload failed"),
			record: []*OptOutRecord{
				{
					ID:           "Id",
					OptOutFileID: "FileId",
					MBI:          "Mbi",
					Status:       Accepted,
				},
			},
			uploader: func(input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) {
				return nil, errors.New("upload failed")
			},
			marshaler: func(v interface{}) ([]byte, error) {
				return nil, nil
			},
		},
		{
			name:   "file_marshaling_fails",
			bucket: "demo-bucket",
			file:   "fake-file",
			err:    errors.New("marshaling failed"),
			record: []*OptOutRecord{
				{
					ID:           "Id",
					OptOutFileID: "FileId",
					MBI:          "Mbi",
					Status:       Accepted,
				},
			},
			uploader: func(input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) {
				return nil, errors.New("upload failed")
			},
			marshaler: func(v interface{}) ([]byte, error) {
				return nil, errors.New("marshaling failed")
			},
		},
	}

	for _, test := range tests {
		fmt.Printf("~~~ %s test\n", test.name)
		err := uploadResponseFile(test.bucket, test.file, test.uploader, test.record, test.marshaler)
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
