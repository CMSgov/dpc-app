package main

import (
	"fmt"
	"os"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/ssm"
	"github.com/aws/aws-sdk-go/service/wafv2"
	log "github.com/sirupsen/logrus"
)

type Parameters struct {
	Id        string
	Name      string
	Scope     string
	LockToken string
	Addresses []string
}

var createSession = func() (*session.Session, error) {
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
	}

	if err != nil {
		return nil, err
	}

	return sess, nil
}

var getAuthDbSecrets = func(dbUser string, dbPassword string) (map[string]string, error) {
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

var updateIpAddresses = func(ipSetName string, ipAddresses []string) ([]string, error) {
	sess, sessErr := createSession()
	if sessErr != nil {
		return nil, fmt.Errorf("failed to create session to update ip set, %v", sessErr)
	}

	wafsvc := wafv2.New(sess, &aws.Config{
		Region: aws.String("us-east-1"),
	})

	listParams := &wafv2.ListIPSetsInput{
		Scope: aws.String("REGIONAL"),
	}
	ipSetList, listErr := wafsvc.ListIPSets(listParams)
	if listErr != nil {
		return nil, fmt.Errorf("failed to fetch ip address sets, %v", listErr)
	}

	log.WithField("name", ipSetName).Info("Fetching IP set")
	getParams := &wafv2.GetIPSetInput{
		Name:  &ipSetName,
		Scope: aws.String("REGIONAL"),
	}
	for _, ipSet := range ipSetList.IPSets {
		if *ipSet.Name == ipSetName {
			getParams.Id = ipSet.Id
			break
		}
	}
	ipSet, getErr := wafsvc.GetIPSet(getParams)
	if getErr != nil {
		return nil, fmt.Errorf("failed to get expected ip address set, %v", getErr)
	}

	updateParams := &wafv2.UpdateIPSetInput{
		Id:          ipSet.IPSet.Id,
		Name:        aws.String(ipSetName),
		Scope:       aws.String("REGIONAL"),
		LockToken:   ipSet.LockToken,
		Addresses:   aws.StringSlice(ipAddresses),
		Description: aws.String("IP ranges for customers of this API"),
	}
	_, updateErr := wafsvc.UpdateIPSet(updateParams)
	if updateErr != nil {
		return nil, fmt.Errorf("failed to update ip address set, %v", updateErr)
	}

	return ipAddresses, nil
}
