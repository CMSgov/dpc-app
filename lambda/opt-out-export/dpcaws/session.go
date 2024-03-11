package dpcaws

import (
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials/stscreds"
	"github.com/aws/aws-sdk-go/aws/session"
)

var s3Region = "us-east-1"
var profile = "755619740999_ct-ado-bcda-application-admin"

// Makes these easily mockable for testing
var newSession = session.NewSession
var newSessionWithOptions = session.NewSessionWithOptions

// NewSession
// Returns a new AWS session using the given roleArn
func NewSession(roleArn string) (*session.Session, error) {
	sess := session.Must(session.NewSession())
	var err error

	if roleArn != "" {
		sess, err = newSession(&aws.Config{
			Region: aws.String(s3Region),
			Credentials: stscreds.NewCredentials(
				sess,
				roleArn,
			),
		})
	} else {
		sess, err = newSession(&aws.Config{
			Region: aws.String(s3Region),
		})
	}

	if err != nil {
		return nil, err
	}

	return sess, nil
}

// NewLocalSession
// Returns a new AWS session by connecting to a remote endpoint.  Primarily used for connecting to a locally running AWS environment,
// so we'll be using the default profile that can be pulled from CloudTamer.
func NewLocalSession(endPoint string) (*session.Session, error) {
	var sess, err = newSessionWithOptions(session.Options{
		Profile: profile,
		Config: aws.Config{
			Region:           aws.String(s3Region),
			S3ForcePathStyle: aws.Bool(true),
			Endpoint:         aws.String(endPoint),
		},
	})

	if err != nil {
		return nil, err
	}
	return sess, nil
}