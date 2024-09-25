package main

import (
	"testing"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/credentials/stscreds"
	"github.com/aws/aws-sdk-go/service/wafv2"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

func TestIntegrationUpdateIpSet(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test.")
	}
	oriGetSecrets := getSecrets
	oriCreateConnection := createConnection
	oriGetAuthData := getAuthData
	oriGetArnValue := getArnValue

	tests := []struct {
		err      error
		mockFunc func()
	}{
		{
			err: nil,
			mockFunc: func() {
				getSecrets = func(dbUser string, dbPassword string) (map[string]string, error) {
					return map[string]string{
						"/dpc/dev/api/db_user_dpc_auth": "db_user_dpc_auth",
						"/dpc/dev/api/db_pass_dpc_auth": "db_pass_dpc_auth",
					}, nil
				}

				getAuthData = func(dbUser string, dbPassword string) ([]string, error) {
					return []string{"127.0.0.1"}, nil
				}

				getArnValue = func() (string, error) {
					return "arn:aws:iam::755619740999:role/delegatedadmin/developer/dpc-dev-api-waf-sync-function", nil
				}
			},
		},
	}

	for _, test := range tests {
		test.mockFunc()
		params, err := updateIpSet()
		assert.NotEmpty(t, params["Addresses"])
		assert.Nil(t, err)

		sess, sessErr := createSession()
		assert.Nil(t, sessErr)
		assumeRoleArn, _ := getArnValue()
		wafsvc := wafv2.New(sess, &aws.Config{
			Region: aws.String("us-east-1"),
			Credentials: stscreds.NewCredentials(
				sess,
				assumeRoleArn,
			),
		})
		log.Info("IP SETS FOUND:")
		ipSetList, listErr := wafsvc.ListIPSets(&wafv2.ListIPSetsInput{Scope: aws.String("CLOUDFRONT")})
		log.Info(ipSetList.IPSets)
		assert.Nil(t, listErr)
		ipSet, wafErr := wafsvc.GetIPSet(&wafv2.GetIPSetInput{
			Id:   aws.String(params["Id"].(string)),
			Name: aws.String(params["Name"].(string)),
		})
		assert.Nil(t, wafErr)
		assert.Equal(t, ipSet.IPSet.Addresses, params["Addresses"])
	}

	getSecrets = oriGetSecrets
	createConnection = oriCreateConnection
	getAuthData = oriGetAuthData
	getArnValue = oriGetArnValue
}
