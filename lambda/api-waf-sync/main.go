package main

import (
	"context"
	"fmt"
	"os"
	"time"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	_ "github.com/lib/pq"
	log "github.com/sirupsen/logrus"
)

type IpAddress struct {
	ip_address string
}

// Allow these to be switched out during unit tests
var getSecrets = getAuthDbSecrets
var updateIpAddresses = updateIPSetInWAF

var isTesting = os.Getenv("IS_TESTING") == "true"

func main() {
	if isTesting {
		var addresses, err = updateIpSet()
		if err != nil {
			log.Error(err)
		} else {
			log.Println(addresses)
		}
	} else {
		lambda.Start(handler)
	}
}

func handler(ctx context.Context, event events.S3Event) ([]string, error) {
	emptySet := []string{}
	log.SetFormatter(&log.JSONFormatter{
		DisableHTMLEscape: true,
		TimestampFormat:   time.RFC3339Nano,
	})
	var ipSet, err = updateIpSet()
	if err != nil {
		return emptySet, err
	}
	log.Info("Successfully completed executing export lambda")
	return ipSet, nil
}

func updateIpSet() ([]string, error) {
	emptySet := []string{}
	ipAddresses := make(map[string]IpAddress)
	ipSetName := fmt.Sprintf("dpc-%s-api-customers", os.Getenv("ENV"))

	authDbUser := fmt.Sprintf("/dpc/%s/auth/db_read_only_user_dpc_auth", os.Getenv("ENV"))
	authDbPassword := fmt.Sprintf("/dpc/%s/auth/db_read_only_pass_dpc_auth", os.Getenv("ENV"))
	secretsInfo, secretErr := getSecrets(authDbUser, authDbPassword)
	if secretErr != nil {
		return emptySet, secretErr
	}

	authDbErr := getAuthData(secretsInfo[authDbUser], secretsInfo[authDbPassword], ipAddresses)
	if authDbErr != nil {
		return emptySet, authDbErr
	}

	var ipAddressArray []string
	for ip := range ipAddresses {
		ipAddressArray = append(ipAddressArray, ip)
	}
	addresses, wafErr := updateIpAddresses(ipSetName, ipAddressArray)
	if wafErr != nil {
		return emptySet, wafErr
	}

	return addresses, nil
}
