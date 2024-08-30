package dpcaws
import (
	"testing"
	"errors"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/stretchr/testify/assert"
)

func TestNewSession(t *testing.T) {
	tests := []struct {
		expect   	*session.Session
		err      	error
		newSession 	func(cfgs ...*aws.Config) (*session.Session, error)
	}{
		{
			// Happy path
			expect: nil,
			err: nil,
			newSession: func(cfgs ...*aws.Config) (*session.Session, error) {return nil, nil},
		},
		{
			// Error returned from NewSession
			expect: nil,
			err: errors.New("error"),
			newSession: func(cfgs ...*aws.Config) (*session.Session, error) {return nil, errors.New("error")},
		},
	}

	for _, test := range tests {
		newSession = test.newSession
		
		s, err := NewSession("fake_arn")

		assert.Equal(t, test.expect, s)
		assert.Equal(t, test.err, err)
	}
}

func TestNewLocalSession(t *testing.T) {
	tests := []struct {
		expect   				*session.Session
		err      				error
		newSessionWithOptions 	func(opts session.Options) (*session.Session, error)
	}{
		{
			// Happy path
			expect: nil,
			err: nil,
			newSessionWithOptions: func(opts session.Options) (*session.Session, error) {return nil, nil},
		},
		{
			// Error returned from NewSessionWithOptions
			expect: nil,
			err: errors.New("error"),
			newSessionWithOptions: func(opts session.Options) (*session.Session, error) {return nil, errors.New("error")},
		},
	}

	for _, test := range tests {
		newSessionWithOptions = test.newSessionWithOptions
		
		s, err := NewLocalSession("fake_endpoint")

		assert.Equal(t, test.expect, s)
		assert.Equal(t, test.err, err)
	}
}