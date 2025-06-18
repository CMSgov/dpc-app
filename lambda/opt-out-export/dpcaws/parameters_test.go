package dpcaws

import (
	"context"
	"errors"
	"fmt"
	"testing"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/ssm"
	"github.com/aws/aws-sdk-go-v2/service/ssm/types"
	"github.com/stretchr/testify/assert"
)

func TestGetParameter(t *testing.T) {
	key1 := "key1"
	parm1 := "parm1"

	tests := []struct {
		keyname            string
		expectedValue      string
		expectedErr        error
		ssmNew             func(cfg aws.Config, optFns ...func(*ssm.Options)) *ssm.Client
		ssmsvcGetParameter func(c *ssm.Client, ctx context.Context, input *ssm.GetParameterInput, optFns ...func(*ssm.Options)) (*ssm.GetParameterOutput, error)
	}{
		{
			// Happy path
			keyname:       key1,
			expectedValue: parm1,
			expectedErr:   nil,
			ssmNew:        func(cfg aws.Config, optFns ...func(*ssm.Options)) *ssm.Client { return nil },
			ssmsvcGetParameter: func(c *ssm.Client, ctx context.Context, input *ssm.GetParameterInput, optFns ...func(*ssm.Options)) (*ssm.GetParameterOutput, error) {
				parm := types.Parameter{
					Name: &key1, Value: &parm1,
				}
				getParametersOutput := ssm.GetParameterOutput{Parameter: &parm}
				return &getParametersOutput, nil
			},
		},
		{
			// GetParameter fails
			keyname:       key1,
			expectedValue: "",
			expectedErr:   errors.New("Error retrieving parameter key1 from parameter store: error"),
			ssmNew:        func(cfg aws.Config, optFns ...func(*ssm.Options)) *ssm.Client { return nil },
			ssmsvcGetParameter: func(c *ssm.Client, ctx context.Context, input *ssm.GetParameterInput, optFns ...func(*ssm.Options)) (*ssm.GetParameterOutput, error) {
				return nil, errors.New("error")
			},
		},
		{
			// Empty parameter
			keyname:       key1,
			expectedValue: "",
			expectedErr:   fmt.Errorf("No parameter store value found for %s", key1),
			ssmNew:        func(cfg aws.Config, optFns ...func(*ssm.Options)) *ssm.Client { return nil },
			ssmsvcGetParameter: func(c *ssm.Client, ctx context.Context, input *ssm.GetParameterInput, optFns ...func(*ssm.Options)) (*ssm.GetParameterOutput, error) {
				val := ""
				parm := types.Parameter{
					Name: &key1, Value: &val,
				}

				getParameterOutput := ssm.GetParameterOutput{Parameter: &parm}
				return &getParameterOutput, nil
			},
		},
	}
	ctx := context.TODO()
	cfg := aws.Config{}
	for _, test := range tests {
		ssmNew = test.ssmNew
		ssmsvcGetParameter = test.ssmsvcGetParameter

		value, err := GetParameter(ctx, cfg, test.keyname)
		assert.Equal(t, test.expectedValue, value)

		if test.expectedErr == nil {
			assert.Nil(t, err)
		} else {
			assert.Equal(t, test.expectedErr.Error(), err.Error())
		}
	}
}


func TestGetParameters(t *testing.T) {
	key1 := "key1"
	key2 := "key2"
	parm1 := "parm1"
	parm2 := "parm2"

	tests := []struct {
		keys                []string
		parms               map[string]string
		err                 error
		ssmNew              func(cfg aws.Config, optFns ...func(*ssm.Options)) *ssm.Client
		ssmsvcGetParameters func(c *ssm.Client, ctx context.Context, input *ssm.GetParametersInput, optFns ...func(*ssm.Options)) (*ssm.GetParametersOutput, error)
	}{
		{
			// Happy path
			keys:   []string{key1, key2},
			parms:  map[string]string{key1: parm1, key2: parm2},
			err:    nil,
			ssmNew: func(cfg aws.Config, optFns ...func(*ssm.Options)) *ssm.Client { return nil },
			ssmsvcGetParameters: func(c *ssm.Client, ctx context.Context, input *ssm.GetParametersInput, optFns ...func(*ssm.Options)) (*ssm.GetParametersOutput, error) {
				parms := []types.Parameter{
					{Name: &key1, Value: &parm1},
					{Name: &key2, Value: &parm2},
				}
				getParametersOutput := ssm.GetParametersOutput{Parameters: parms}
				return &getParametersOutput, nil
			},
		},
		{
			// GetParameters fails
			keys:   []string{key1, key2},
			parms:  nil,
			err:    errors.New("error connecting to parameter store: error"),
			ssmNew: func(cfg aws.Config, optFns ...func(*ssm.Options)) *ssm.Client { return nil },
			ssmsvcGetParameters: func(c *ssm.Client, ctx context.Context, input *ssm.GetParametersInput, optFns ...func(*ssm.Options)) (*ssm.GetParametersOutput, error) {
				return nil, errors.New("error")
			},
		},
		{
			// Invalid parameter
			keys:   []string{key1, key2},
			parms:  nil,
			err:    fmt.Errorf("invalid parameters error: %s,\n", key2),
			ssmNew: func(cfg aws.Config, optFns ...func(*ssm.Options)) *ssm.Client { return nil },
			ssmsvcGetParameters: func(c *ssm.Client, ctx context.Context, input *ssm.GetParametersInput, optFns ...func(*ssm.Options)) (*ssm.GetParametersOutput, error) {
				parms := []types.Parameter{
					{Name: &key1, Value: &parm1},
				}
				invalidParms := []string{key2}

				getParametersOutput := ssm.GetParametersOutput{Parameters: parms, InvalidParameters: invalidParms}
				return &getParametersOutput, nil
			},
		},
	}

	ctx := context.TODO()
	cfg := aws.Config{}
	for _, test := range tests {
		ssmNew = test.ssmNew
		ssmsvcGetParameters = test.ssmsvcGetParameters

		parms, err := GetParameters(ctx, cfg, test.keys)

		assert.Equal(t, test.parms, parms)
		assert.Equal(t, test.err, err)
	}
}
