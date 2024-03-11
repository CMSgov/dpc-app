package dpcaws

import (
	"testing"
	"os"
	"errors"

	"github.com/aws/aws-sdk-go/aws/client"
	"github.com/aws/aws-sdk-go/service/s3/s3manager"
	"github.com/stretchr/testify/assert"
)

func TestUploadFileToS3(t *testing.T) {
	tests := []struct {
		err      	error
		osOpen	 	func(name string) (*os.File, error)
		newUploader	func(c client.ConfigProvider, options ...func(*s3manager.Uploader)) (*s3manager.Uploader)
		upload 		func(u s3manager.Uploader, input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error)
	}{
		{
			// Happy path
			err: 			nil,
			osOpen: 		func(name string) (*os.File, error) {return nil, nil},
			newUploader: 	func(c client.ConfigProvider, options ...func(*s3manager.Uploader)) (*s3manager.Uploader) { 
				s3Uploader := s3manager.Uploader{}
				return &s3Uploader
			},
			upload:			func(u s3manager.Uploader, input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) { return nil, nil },
		},
		{
			// Error opening file
			err: 			errors.New("failed to open file, error"),
			osOpen: 		func(name string) (*os.File, error) {return nil, errors.New("error")},
			newUploader: 	func(c client.ConfigProvider, options ...func(*s3manager.Uploader)) (*s3manager.Uploader) { return nil },
			upload:			func(u s3manager.Uploader, input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) { return nil, nil },
		},
		{
			// Error uploading file
			err: 			errors.New("failed to upload file, error"),
			osOpen: 		func(name string) (*os.File, error) {return nil, nil},
			newUploader: 	func(c client.ConfigProvider, options ...func(*s3manager.Uploader)) (*s3manager.Uploader) { 
				s3Uploader := s3manager.Uploader{}
				return &s3Uploader
			},
			upload:			func(u s3manager.Uploader, input *s3manager.UploadInput, options ...func(*s3manager.Uploader)) (*s3manager.UploadOutput, error) { 
				return nil, errors.New("error") 
			},
		},
	}

	for _, test := range tests {
		osOpen = test.osOpen
		newUploader = test.newUploader
		upload = test.upload
		
		err := UploadFileToS3(nil, "file", "bucket", "path")

		assert.Equal(t, test.err, err)
	}
}