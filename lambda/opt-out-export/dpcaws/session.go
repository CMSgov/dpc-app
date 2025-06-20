package dpcaws

import (
	"context"
	
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/credentials/stscreds"
	"github.com/aws/aws-sdk-go-v2/service/sts"
	"github.com/aws/smithy-go/logging"
)

var s3Region = "us-east-1"

// Returns a new AWS Config using the given roleArn
func NewSession(ctx context.Context, roleArn string) (aws.Config, error) {
	cfg, err := config.LoadDefaultConfig(ctx, config.WithRegion(s3Region), config.WithLogger(logging.Nop{}))
	if err != nil {
		return cfg, err
	}
	if roleArn != "" {
		client := sts.NewFromConfig(cfg)
		creds := stscreds.NewAssumeRoleProvider(client, roleArn)
		cfg.Credentials = aws.NewCredentialsCache(creds)
	}

	return cfg, nil
}

// Returns a new AWS Config by connecting to a remote endpoint.  Primarily used for connecting to a locally running AWS environment,
func NewLocalSession(ctx context.Context, endPoint string) (aws.Config, error) {
	return config.LoadDefaultConfig(ctx,
		config.WithRegion(s3Region),
		config.WithEndpointResolver(
			aws.EndpointResolverFunc(func(service, region string) (aws.Endpoint, error) {
				return aws.Endpoint{
					PartitionID:   "aws",
					URL:           endPoint,
					SigningRegion: s3Region,
				}, nil
			}),
		),
	)
}
