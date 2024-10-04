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

func handler(ctx context.Context, event events.S3Event) (string, error) {
	log.SetFormatter(&log.JSONFormatter{
		DisableHTMLEscape: true,
		TimestampFormat:   time.RFC3339Nano,
	})
	var params, err = updateIpSet()
	if err != nil {
		return "", err
	}
	log.Info("Successfully completed executing export lambda")
	return params["Addresses"], nil
}

var updateIpSet = func() (map[string]string, error) {
	params := map[string]string{"Addresses": ""}
	ipSetName := fmt.Sprintf("dpc-%s-api-customers", os.Getenv("ENV"))

	authDbUser := fmt.Sprintf("/dpc/%s/api/db_read_only_user_dpc_auth", os.Getenv("ENV"))
	authDbPassword := fmt.Sprintf("/dpc/%s/api/db_read_only_pass_dpc_auth", os.Getenv("ENV"))
	secretsInfo, secretErr := getAuthDbSecrets(authDbUser, authDbPassword)
	if secretErr != nil {
		return nil, secretErr
	}

	ipAddresses, authDbErr := getAuthData(secretsInfo[authDbUser], secretsInfo[authDbPassword])
	if authDbErr != nil {
		return nil, authDbErr
	}

	params, wafErr := updateIpAddresses(ipSetName, ipAddresses)
	if wafErr != nil {
		return nil, wafErr
	}

	return params, nil
}
