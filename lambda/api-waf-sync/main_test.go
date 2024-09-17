package main

import (
	"bytes"
	"bufio"
	"database/sql"
	"fmt"
	"os"
	"strings"
	"testing"
	"time"

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
				getSecrets = func(s *session.Session, keynames []*string) (map[string]string, error) {
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
		params, err := updateIpSet()
		assert.NotEmpty(t, params["Addresses"])
		assert.Nil(t, err)

        updatedAddresses = params["Addresses"]
        delete(params, "Addresses")
		ipSet, wafErr := wafv2.GetIPSet(params)
		assert.Nil(t, wafErr)
		assert.Equal(t, ipSet["Addresses"], updatedAddresses)
	}

	getSecrets = oriGetSecrets
	createConnection = oriCreateConnection
	getAuthData = oriGetAuthData
}
