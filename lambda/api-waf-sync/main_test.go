package main

import (
	"context"
	"testing"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/wafv2"
	"github.com/stretchr/testify/assert"
)

func TestIntegrationUpdateIpSet(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test.")
	}
	oriGetSecrets := getAuthDbSecrets
	oriGetAuthData := getAuthData

	tests := []struct {
		expected []string
		mockFunc func()
	}{
		{
			expected: []string{"127.0.0.1/32"},
			mockFunc: func() {
				getAuthDbSecrets = func(ctx context.Context, dbUser string, dbPassword string) (map[string]string, error) {
					return map[string]string{
						"/dpc/dev/api/db_user_dpc_auth": "db_user_dpc_auth",
						"/dpc/dev/api/db_pass_dpc_auth": "db_pass_dpc_auth",
					}, nil
				}

				getAuthData = func(dbUser string, dbPassword string) ([]string, error) {
					return []string{"127.0.0.1/32"}, nil
				}
			},
		},
		{
			expected: []string{"127.0.0.1/32", "127.0.0.2/32"},
			mockFunc: func() {
				getAuthDbSecrets = func(ctx context.Context, dbUser string, dbPassword string) (map[string]string, error) {
					return map[string]string{
						"/dpc/dev/api/db_user_dpc_auth": "db_user_dpc_auth",
						"/dpc/dev/api/db_pass_dpc_auth": "db_pass_dpc_auth",
					}, nil
				}

				getAuthData = func(dbUser string, dbPassword string) ([]string, error) {
					return []string{"127.0.0.1/32", "127.0.0.2/32"}, nil
				}
			},
		},
	}

	for _, test := range tests {
		ctx := context.TODO()
		cfg, cfgErr := createConfig(ctx)
		assert.Nil(t, cfgErr)
		wafsvc := wafv2.NewFromConfig(cfg, func(o *wafv2.Options) {
			o.Region = "us-east-1"
		})

		// Get current IP set and save existing addresses
		dpcSetName := "dpc-test-api-customers"
		ipSetList, listErr := wafsvc.ListIPSets(ctx, &wafv2.ListIPSetsInput{Scope: "REGIONAL"})
		assert.Nil(t, listErr)
		var ipSetId string
		for _, set := range ipSetList.IPSets {
			if *set.Name == dpcSetName {
				ipSetId = *set.Id
				break
			}
		}
		assert.NotNil(t, ipSetId)
		ipSet, wafErr := wafsvc.GetIPSet(ctx, &wafv2.GetIPSetInput{
			Id:    &ipSetId,
			Name:  &dpcSetName,
			Scope: "REGIONAL",
		})
		assert.Nil(t, wafErr)
		oriIpAddresses := ipSet.IPSet.Addresses

		// Update IP set with new addresses and verify
		test.mockFunc()
		addrs, err := updateIpSet(ctx)
		assert.Equal(t, test.expected, addrs)
		assert.Nil(t, err)

		// Reset original IP addresses and verify
		ipSet, wafErr = wafsvc.GetIPSet(ctx, &wafv2.GetIPSetInput{
			Id:    &ipSetId,
			Name:  &dpcSetName,
			Scope: "REGIONAL",
		})
		assert.Nil(t, wafErr)
		_, updateErr := wafsvc.UpdateIPSet(ctx, &wafv2.UpdateIPSetInput{
			Id:          ipSet.IPSet.Id,
			Name:        aws.String(dpcSetName),
			Scope:       "REGIONAL",
			LockToken:   ipSet.LockToken,
			Addresses:   oriIpAddresses,
			Description: aws.String("IP ranges for customers of this API"),
		})
		assert.Nil(t, updateErr)
		ipSet, wafErr = wafsvc.GetIPSet(ctx, &wafv2.GetIPSetInput{
			Id:    &ipSetId,
			Name:  &dpcSetName,
			Scope: "REGIONAL",
		})
		assert.Nil(t, wafErr)
		assert.Equal(t, ipSet.IPSet.Addresses, oriIpAddresses)
	}

	getAuthDbSecrets = oriGetSecrets
	getAuthData = oriGetAuthData
}
