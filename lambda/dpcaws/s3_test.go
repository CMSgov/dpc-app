package dpcaws

import (
	"bytes"
	"errors"
	"testing"

	"github.com/aws/aws-sdk-go/aws/client"
	"github.com/aws/aws-sdk-go/service/s3/s3manager"
	"github.com/stretchr/testify/assert"
)

func TestUploadFileToS3(t *testing.T) {
	tests := []struct {
		err         error
		newUploader func(c client.ConfigProvider, options ...func(*s3manager.Uploader)) *s3manager.Uploader
		upload      func(u s3manager.Uploader, input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error)
	}{
		{
			// Happy path
			err: nil,
			newUploader: func(c client.ConfigProvider, options ...func(*s3manager.Uploader)) *s3manager.Uploader {
				s3Uploader := s3manager.Uploader{}
				return &s3Uploader
			},
			upload: func(u s3manager.Uploader, input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) {
				return nil, nil
			},
		},
		{
			// Error uploading file
			err: errors.New("failed to upload file, error"),
			newUploader: func(c client.ConfigProvider, options ...func(*s3manager.Uploader)) *s3manager.Uploader {
				s3Uploader := s3manager.Uploader{}
				return &s3Uploader
			},
			upload: func(u s3manager.Uploader, input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) {
				return nil, errors.New("error")
			},
		},
	}

	for _, test := range tests {
		newUploader = test.newUploader
		upload = test.upload

		var buff bytes.Buffer
		err := UploadFileToS3(nil, "file", buff, "bucket", "path")
		assert.Equal(t, test.err, err)
	}
}
