package dpcaws

import (
	"bytes"
	"fmt"
	"os"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/s3/s3manager"
)

// Makes these easily mockable
var osOpen = os.Open
var newUploader = s3manager.NewUploader
var upload = s3manager.Uploader.Upload

// AddFileToS3
// Uses the given sessions to upload the file to the given s3Bucket
func UploadFileToS3(s *session.Session, fileName string, buff bytes.Buffer, s3Bucket string, s3Path string) error {
	// Upload file to bucket
	uploader := newUploader(s)

	_, s3Err := upload(*uploader, &s3manager.UploadInput{
		Bucket: aws.String(s3Bucket),
		Key:    aws.String(s3Path + "/" + fileName),
		Body:   bytes.NewReader(buff.Bytes()),
	})

	if s3Err != nil {
		return fmt.Errorf("failed to upload file, %v", s3Err)
	}
	return nil
}