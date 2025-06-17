package dpcaws

import (
	"bytes"
	"context"
	"fmt"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/aws/aws-sdk-go-v2/feature/s3/manager"
)

// Makes these easily mockable
var newUploader = manager.NewUploader
var upload = manager.Uploader.Upload

// AddFileToS3
// Uses the given sessions to upload the file to the given s3Bucket
func UploadFileToS3(ctx context.Context, cfg aws.Config, fileName string, buff bytes.Buffer, s3Bucket string, s3Path string) error {
	// Upload file to bucket
	client := s3.NewFromConfig(cfg, func(o *s3.Options){ o.UsePathStyle = true })
	uploader := newUploader(client)

	_, s3Err := upload(*uploader, ctx, &s3.PutObjectInput{
		Bucket: aws.String(s3Bucket),
		Key:    aws.String(s3Path + "/" + fileName),
		Body:   bytes.NewReader(buff.Bytes()),
	})

	if s3Err != nil {
		return fmt.Errorf("failed to upload file, %v", s3Err)
	}
	return nil
}

func DownloadFileFromS3(ctx context.Context, cfg aws.Config, bucket string, file string) ([]byte, error) {
	client := s3.NewFromConfig(cfg, func(o *s3.Options){ o.UsePathStyle = true })
	downloader := manager.NewDownloader(client)
	buff := manager.NewWriteAtBuffer([]byte{})
	_, err := downloader.Download(ctx, buff, &s3.GetObjectInput{
		Bucket: aws.String(bucket),
		Key:    aws.String(file),
	})

	return buff.Bytes(), err
}
