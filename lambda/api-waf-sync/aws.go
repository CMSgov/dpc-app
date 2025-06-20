package main

import (
	"fmt"
	"os"

	"context"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/ssm"
	"github.com/aws/aws-sdk-go-v2/service/wafv2"
	log "github.com/sirupsen/logrus"
	"github.com/aws/smithy-go/logging"
)

type Parameters struct {
	Id        string
	Name      string
	Scope     string
	LockToken string
	Addresses []string
}

var createConfig = func(ctx context.Context) (aws.Config, error) {
	if isTesting {
		return config.LoadDefaultConfig(ctx,
			config.WithSharedConfigProfile("default"),
			config.WithRegion("us-east-1"),
			config.WithEndpointResolver(
				aws.EndpointResolverFunc(func(service, region string) (aws.Endpoint, error) {
					return aws.Endpoint{
						PartitionID:   "aws",
						URL:           "http://localstack:4566",
						SigningRegion: "us-east-1",
					}, nil
				}),
			),
		)
	}
	return config.LoadDefaultConfig(ctx, config.WithLogger(logging.Nop{}))
}

var getAuthDbSecrets = func(ctx context.Context, dbUser string, dbPassword string) (map[string]string, error) {
	secretsInfo := make(map[string]string)

	if isTesting {
		secretsInfo[dbUser] = os.Getenv("DB_USER_DPC_AUTH")
		secretsInfo[dbPassword] = os.Getenv("DB_PASS_DPC_AUTH")
	} else {
		cfg, cfgErr := createConfig(ctx)
		if cfgErr != nil {
			return nil, fmt.Errorf("failed to create session to update ip set, %v", cfgErr)
		}
		var keynames []string = make([]string, 2)
		keynames[0] = dbUser
		keynames[1] = dbPassword
		ssmsvc := ssm.NewFromConfig(cfg, func(o *ssm.Options) {
			o.Region = "us-east-1"
		})

		withDecryption := true
		params, err := ssmsvc.GetParameters(ctx, &ssm.GetParametersInput{
			Names:          keynames,
			WithDecryption: &withDecryption,
		})
		if err != nil {
			return nil, fmt.Errorf("getAuthDbSecrets: Error connecting to parameter store: %w", err)
		}

		// Unknown keys will come back as invalid, make sure we error on them
		if len(params.InvalidParameters) > 0 {
			invalidParamsStr := ""
			for i := 0; i < len(params.InvalidParameters); i++ {
				invalidParamsStr += fmt.Sprintf("%s,\n", params.InvalidParameters[i])
			}
			return nil, fmt.Errorf("invalid parameters error: %s", invalidParamsStr)
		}

		for _, item := range params.Parameters {
			secretsInfo[*item.Name] = *item.Value
		}
	}
	return secretsInfo, nil
}

var updateIpAddresses = func(ctx context.Context, ipSetName string, ipAddresses []string) ([]string, error) {
	cfg, cfgErr := createConfig(ctx)
	if cfgErr != nil {
		return nil, fmt.Errorf("failed to create session to update ip set, %v", cfgErr)
	}

	wafsvc := wafv2.NewFromConfig(cfg, func(o *wafv2.Options) {
		o.Region = "us-east-1"
	})

	listParams := &wafv2.ListIPSetsInput{
		Scope: "REGIONAL",
	}
	ipSetList, listErr := wafsvc.ListIPSets(ctx, listParams)
	if listErr != nil {
		return nil, fmt.Errorf("failed to fetch ip address sets, %v", listErr)
	}

	log.WithField("name", ipSetName).Info("Fetching IP set")
	getParams := &wafv2.GetIPSetInput{
		Name:  &ipSetName,
		Scope: "REGIONAL",
	}
	for _, ipSet := range ipSetList.IPSets {
		if *ipSet.Name == ipSetName {
			getParams.Id = ipSet.Id
			break
		}
	}
	ipSet, getErr := wafsvc.GetIPSet(ctx, getParams)
	if getErr != nil {
		return nil, fmt.Errorf("failed to get expected ip address set, %v", getErr)
	}

	updateParams := &wafv2.UpdateIPSetInput{
		Id:          ipSet.IPSet.Id,
		Name:        aws.String(ipSetName),
		Scope:       "REGIONAL",
		LockToken:   ipSet.LockToken,
		Addresses:   ipAddresses,
		Description: aws.String("IP ranges for customers of this API"),
	}
	_, updateErr := wafsvc.UpdateIPSet(ctx, updateParams)
	if updateErr != nil {
		return nil, fmt.Errorf("failed to update ip address set, %v", updateErr)
	}

	addrs := []string{}
	ipSet, getErr = wafsvc.GetIPSet(ctx, getParams)
	if getErr != nil {
		return nil, fmt.Errorf("failed to get expected ip address set, %v", getErr)
	}
	for _, addr := range ipSet.IPSet.Addresses {
		addrs = append(addrs, addr)
	}
	return addrs, nil
}
