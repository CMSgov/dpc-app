package dpcaws

import (
	"bytes"
	"context"
	"errors"
	"testing"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/feature/s3/manager"
	"github.com/stretchr/testify/assert"
)

func TestUploadFileToS3(t *testing.T) {
	tests := []struct {
		err         error
	//	 NewUploader(client UploadAPIClient, options ...func(*Uploader)) *Uploader
		newUploader func(c manager.UploadAPIClient, options ...func(*manager.Uploader)) *manager.Uploader
		upload      func(u manager.Uploader, ctx context.Context, input *s3.PutObjectInput, options ...func(*manager.Uploader)) (*manager.UploadOutput, error)
	}{
		{
			// Happy path
			err: nil,
			newUploader: func(c manager.UploadAPIClient, options ...func(*manager.Uploader)) *manager.Uploader {
				s3Uploader := manager.Uploader{}
				return &s3Uploader
			},
			upload: func(u manager.Uploader, ctx context.Context, input *s3.PutObjectInput, options ...func(*manager.Uploader)) (*manager.UploadOutput, error) {
				return nil, nil
			},
		},
		{
			// Error uploading file
			err: errors.New("failed to upload file, error"),
			newUploader: func(c manager.UploadAPIClient, options ...func(*manager.Uploader)) *manager.Uploader {
				s3Uploader := manager.Uploader{}
				return &s3Uploader
			},
			upload: func(u manager.Uploader, ctx context.Context, input *s3.PutObjectInput, options ...func(*manager.Uploader)) (*manager.UploadOutput, error) {
				return nil, errors.New("error")
			},
		},
	}
	ctx := context.TODO()
	cfg := aws.Config{}

	for _, test := range tests {
		newUploader = test.newUploader
		upload = test.upload

		var buff bytes.Buffer
		err := UploadFileToS3(ctx, cfg, "file", buff, "bucket", "path")
		assert.Equal(t, test.err, err)
	}
}
