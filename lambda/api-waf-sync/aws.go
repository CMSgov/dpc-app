package main

import (
	"fmt"
	"os"

    "github.com/aws/aws-sdk-go/aws"
    "github.com/aws/aws-sdk-go/aws/credentials/stscreds"
    "github.com/aws/aws-sdk-go/aws/session"
    "github.com/aws/aws-sdk-go/service/ssm"
	"github.com/aws/aws-sdk-go/service/wafv2"
)

type Parameters struct {
    Id string
    Name string
    Scope string
    LockToken string
    Addresses []string
}

var listIpSetsWaf = wafv2.ListIpSets
var getIpSetWaf = wafv2.GetIpSet
var updateIpSetWaf = wafv2.UpdateIPSet

func createSession() (*session.Session, error) {
	sess := session.Must(session.NewSession())
	var err error
	if isTesting {
		sess, err = session.NewSessionWithOptions(session.Options{
			Profile: "default",
			Config: aws.Config{
				Region:           aws.String("us-east-1"),
				S3ForcePathStyle: aws.Bool(true),
				Endpoint:         aws.String("http://localhost:4566"),
			},
		})
	} else {
		assumeRoleArn, err := getAssumeRoleArn()

		if err == nil {
			sess, err = session.NewSession(&aws.Config{
				Region: aws.String("us-east-1"),
				Credentials: stscreds.NewCredentials(
					sess,
					assumeRoleArn,
				),
			})
		}
	}

	if err != nil {
		return nil, err
	}

	return sess, nil
}

func getAssumeRoleArn() (string, error) {
	if isTesting {
		val := os.Getenv("AWS_ASSUME_ROLE_ARN")
		if val == "" {
			return "", fmt.Errorf("AWS_ASSUME_ROLE_ARN must be set during testing")
		}

		return val, nil
	}

	parameterName := fmt.Sprintf("/api-waf-sync/dpc/%s/bfd-bucket-role-arn", os.Getenv("ENV"))

	var keynames []*string = make([]*string, 1)
	keynames[0] = &parameterName

	sess, err := session.NewSession(&aws.Config{
		Region: aws.String("us-east-1"),
	})

	if err != nil {
		return "", fmt.Errorf("getAssumeRoleArn: Error creating AWS session: %w", err)
	}

	ssmsvc := ssm.New(sess)

	withDecryption := true
	result, err := ssmsvc.GetParameter(&ssm.GetParameterInput{
		Name:           &parameterName,
		WithDecryption: &withDecryption,
	})

	if err != nil {
		return "", fmt.Errorf("getAssumeRoleArn: Error connecting to parameter store: %w", err)
	}

	arn := *result.Parameter.Value

	if arn == "" {
		return "", fmt.Errorf("getAssumeRoleArn: No value found for bfd-bucket-role-arn")
	}

	return arn, nil
}

func getAuthDbSecrets(dbUser string, dbPassword string) (map[string]string, error) {
    secretsInfo := make(map[string]string)
    if isTesting {
        secretsInfo[dbUser] = os.Getenv("DB_USER_DPC_AUTH")
        secretsInfo[dbPassword] = os.Getenv("DB_PASS_DPC_AUTH")
    } else {
        var keynames []*string = make([]*string, 2)
        keynames[0] = &dbUser
        keynames[1] = &dbPassword

        sess, err := session.NewSession(&aws.Config{
            Region: aws.String("us-east-1"),
        })
        if err != nil {
            return nil, fmt.Errorf("getAuthDbSecrets: Error creating AWS session: %w", err)
        }
        ssmsvc := ssm.New(sess)

        withDecryption := true
        params, err := ssmsvc.GetParameters(&ssm.GetParametersInput{
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
                invalidParamsStr += fmt.Sprintf("%s,\n", *params.InvalidParameters[i])
            }
            return nil, fmt.Errorf("invalid parameters error: %s", invalidParamsStr)
        }

        for _, item := range params.Parameters {
            secretsInfo[*item.Name] = *item.Value
        }
    }
	return secretsInfo, nil
}

func updateIPSetInWAF(ipSetName string, ipAddresses []string) (error, Parameters) {
    params := Parameters{Scope: "CLOUDFRONT"}
    ipSetList, listErr := listIpSetsWaf(params)
    if listErr != nil {
		return fmt.Errorf("failed to fetch ip address sets, %v", listErr), params
    }

    for _, ipSet := range ipSetList {
        if ipSet["Name"] == ipSetName {
            params.Id = ipSet["Id"]
            break;
        }
    }
    params.Name = ipSetName
    ipSet, getErr := getIpSetWaf(params)
    if getErr != nil {
        return fmt.Errorf("failed to get expected ip address set, %v", getErr), params
    }

    params.LockToken = ipSet["LockToken"]
    params.Addresses = ipAddresses
    _, updateErr := updateIpSetWaf(params)
    if updateErr != nil {
    	return fmt.Errorf("failed to update ip address set, %v", updateErr), params
    }

    return nil, params
}
