package main

import (
	"testing"

	"github.com/aws/aws-sdk-go/service/wafv2"
	"github.com/stretchr/testify/assert"
)

func TestIntegrationUpdateIpSet(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test.")
	}
	oriGetSecrets := getSecrets
	oriCreateConnection := createConnection
	oriGetAuthData := getAuthData

	tests := []struct {
		err      error
		mockFunc func()
	}{
		{
			err: nil,
			mockFunc: func() {
				getSecrets = func(dbUser string, dbPassword string) (map[string]string, error) {
					return map[string]string{
						"/dpc/dev/auth/db_user_dpc_auth": "db_user_dpc_auth",
						"/dpc/dev/auth/db_pass_dpc_auth": "db_pass_dpc_auth",
					}, nil
				}

				getAuthData = func(dbUser string, dbPassword string, ipAddresses map[string]IpAddress) error {
					return nil
				}
			},
		},
	}

	for _, test := range tests {
		test.mockFunc()
		addresses, err := updateIpSet()
		assert.NotEmpty(t, addresses)
		assert.Nil(t, err)

		sess, sessErr := createSession()
		assert.Nil(t, sessErr)
		wafsvc := wafv2.New(sess)
		ipSet, wafErr := (*wafv2.WAFV2).GetIPSet(wafsvc, &wafv2.GetIPSetInput{})
		assert.Nil(t, wafErr)
		assert.Equal(t, ipSet.IPSet.Addresses, addresses)
	}

	getSecrets = oriGetSecrets
	createConnection = oriCreateConnection
	getAuthData = oriGetAuthData
}
