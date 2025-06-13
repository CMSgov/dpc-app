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
)

type Parameters struct {
	Id        string
	Name      string
	Scope     string
	LockToken string
	Addresses []string
}

var createSession = func() (aws.Config, error) {
	cfg, err := config.LoadDefaultConfig(context.TODO())

	if isTesting {
		cfg, err = config.LoadDefaultConfig(context.TODO(),
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

	if err != nil {
		return cfg, err
	}

	return cfg, nil
}

var getAuthDbSecrets = func(dbUser string, dbPassword string) (map[string]string, error) {
	secretsInfo := make(map[string]string)

	if isTesting {
		secretsInfo[dbUser] = os.Getenv("DB_USER_DPC_AUTH")
		secretsInfo[dbPassword] = os.Getenv("DB_PASS_DPC_AUTH")
	} else {
		sess, sessErr := createSession()
		if sessErr != nil {
			return nil, fmt.Errorf("failed to create session to update ip set, %v", sessErr)
		}
		var keynames []string = make([]string, 2)
		keynames[0] = dbUser
		keynames[1] = dbPassword
		ssmsvc := ssm.NewFromConfig(sess, func(o *ssm.Options) {
			o.Region = "us-east-1"
		})

		withDecryption := true
		params, err := ssmsvc.GetParameters(context.TODO(), &ssm.GetParametersInput{
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

var updateIpAddresses = func(ipSetName string, ipAddresses []string) ([]string, error) {
	sess, sessErr := createSession()
	if sessErr != nil {
		return nil, fmt.Errorf("failed to create session to update ip set, %v", sessErr)
	}

	wafsvc := wafv2.NewFromConfig(sess, func(o *wafv2.Options) {
		o.Region = "us-east-1"
	})

	listParams := &wafv2.ListIPSetsInput{
		Scope: "REGIONAL",
	}
	ipSetList, listErr := wafsvc.ListIPSets(context.TODO(), listParams)
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
	ipSet, getErr := wafsvc.GetIPSet(context.TODO(), getParams)
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
	_, updateErr := wafsvc.UpdateIPSet(context.TODO(), updateParams)
	if updateErr != nil {
		return nil, fmt.Errorf("failed to update ip address set, %v", updateErr)
	}

	addrs := []string{}
	ipSet, getErr = wafsvc.GetIPSet(context.TODO(), getParams)
	if getErr != nil {
		return nil, fmt.Errorf("failed to get expected ip address set, %v", getErr)
	}
	for _, addr := range ipSet.IPSet.Addresses {
		addrs = append(addrs, addr)
	}
	return addrs, nil
}
