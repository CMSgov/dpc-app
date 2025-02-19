package main

import (
	"errors"
	"testing"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/stretchr/testify/assert"
)

func TestNewSession(t *testing.T) {
	tests := []struct {
		err        error
		newSession func(cfgs ...*aws.Config) (*session.Session, error)
	}{
		{
			// Happy path
			err:        nil,
			newSession: func(cfgs ...*aws.Config) (*session.Session, error) { return nil, nil },
		},
		{
			// Error returned from NewSession
			err:        errors.New("error"),
			newSession: func(cfgs ...*aws.Config) (*session.Session, error) { return nil, errors.New("error") },
		},
	}

	for _, test := range tests {
		newSession := test.newSession
		sess, err := newSession(aws.NewConfig())

		assert.Nil(t, sess)
		assert.Equal(t, test.err, err)
	}
}

func TestNewLocalSession(t *testing.T) {
	tests := []struct {
		err                   error
		newSessionWithOptions func(opts session.Options) (*session.Session, error)
	}{
		{
			// Happy path
			err:                   nil,
			newSessionWithOptions: func(opts session.Options) (*session.Session, error) { return nil, nil },
		},
		{
			// Error returned from NewSessionWithOptions
			err:                   errors.New("error"),
			newSessionWithOptions: func(opts session.Options) (*session.Session, error) { return nil, errors.New("error") },
		},
	}

	for _, test := range tests {
		newSessionWithOptions := test.newSessionWithOptions

		sess, err := newSessionWithOptions(session.Options{})

		assert.Nil(t, sess)
		assert.Equal(t, test.err, err)
	}
}
