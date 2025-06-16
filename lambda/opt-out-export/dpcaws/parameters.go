package dpcaws

import (
	"context"
	"fmt"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/ssm"
)

// Makes this easier to mock and unit test

func GetParameter(ctx context.Context, cfg aws.Config, keyname string) (string, error) {
	ssmsvc := ssm.NewFromConfig(cfg)

	withDecryption := true
	result, err := ssmsvc.GetParameter(ctx, &ssm.GetParameterInput{
		Name:           &keyname,
		WithDecryption: &withDecryption,
	})

	if err != nil {
		return "", fmt.Errorf("Error retrieving parameter %s from parameter store: %w", keyname, err)
	}

	val := *result.Parameter.Value

	if val == "" {
		return "", fmt.Errorf("No parameter store value found for %s", keyname)
	}

	return val, nil
}

// Returns a list of parameters from the SSM Parameter Store
func GetParameters(ctx context.Context, cfg aws.Config, keynames []string) (map[string]string, error) {
	// Create an SSM client and pull down keys from the param store
	ssmsvc := ssm.NewFromConfig(cfg)

	withDecryption := true
	params, err := ssmsvc.GetParameters(ctx, &ssm.GetParametersInput{
		Names:          keynames,
		WithDecryption: &withDecryption,
	})
	if err != nil {
		return nil, fmt.Errorf("error connecting to parameter store: %s", err)
	}

	// Unknown keys will come back as invalid, make sure we error on them
	if len(params.InvalidParameters) > 0 {
		invalidParamsStr := ""
		for i := 0; i < len(params.InvalidParameters); i++ {
			invalidParamsStr += fmt.Sprintf("%s,\n", params.InvalidParameters[i])
		}
		return nil, fmt.Errorf("invalid parameters error: %s", invalidParamsStr)
	}

	// Build the parameter map that we're going to return
	var paramMap map[string]string = make(map[string]string)

	for _, item := range params.Parameters {
		paramMap[*item.Name] = *item.Value
	}
	return paramMap, nil
}
