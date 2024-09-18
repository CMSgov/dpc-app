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
var getSecrets = dpcaws.GetParameters

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
	log.SetFormatter(&log.JSONFormatter{
		DisableHTMLEscape: true,
		TimestampFormat:   time.RFC3339Nano,
	})
	var ipSet, err = updateIpSet()
	if err != nil {
		return [], err
	}
	log.Info("Successfully completed executing export lambda")
	return ipSet, nil
}

func updateIpSet() ([]string, error) {
    ipAddresses := make(map[string]IpAddress)
    ipSetName := fmt.Sprintf("dpc-%s-api-customers", os.Getenv("ENV"))

    authDbUser := fmt.Sprintf("/dpc/%s/auth/db_read_only_user_dpc_auth", os.Getenv("ENV"))
    authDbPassword := fmt.Sprintf("/dpc/%s/auth/db_read_only_pass_dpc_auth", os.Getenv("ENV"))
    var keynames []*string = make([]*string, 2)
    keynames[0] = &authDbUser
    keynames[1] = &authDbPassword

    secretsInfo, pmErr := getSecrets(session, keynames)
    if pmErr != nil {
        return [], pmErr
    }

    authDbErr := getAuthData(secretsInfo[authDbUser], secretsInfo[authDbPassword], ipAddresses)
    if authDbErr != nil {
        return [], authDbErr
    }

    wafErr, params := UpdateIPSetInWAF(ipSetName, ipAddresses)
    if wafErr != nil {
        return [], wafErr
    }

    return params, nil
}
