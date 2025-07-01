package main

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"time"

	log "github.com/sirupsen/logrus"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/ssm"
	"github.com/aws/smithy-go/logging"
	"github.com/pkg/errors"
)

var isTesting = os.Getenv("IS_TESTING") == "true"

// Used for dependency injection.  Allows us to easily mock these function in unit tests.
func main() {
	if isTesting {
		// test
	} else {
		lambda.Start(handler)
	}
}

func handler(ctx context.Context, event events.SQSEvent) error {
	log.SetFormatter(&log.JSONFormatter{
		DisableHTMLEscape: true,
		TimestampFormat:   time.RFC3339Nano,
	})

	webhook, err := GetParameter(ctx, "/dpc/lambda/slack_webhook_url")
	if err != nil {
		log.Errorf("Unable to retrieve webook url: %v", err)
		return err
	}

	for _, record := range event.Records {
		err := processRecord(record)
		if err != nil {
			log.Errorf("Unable to process SQS Record: %v", err)
			return err
		}
		sendMessageToSlack(webhook, []byte{})
	}
	log.Print("Successfully processed SQS Event")
	return nil
}

func processRecord(record events.SQSMessage) error {
	alarm, err := ParseSQSRecord(record)
	if err != nil || alarm == nil {
		return err
	}

	return nil
}

func sendMessageToSlack(webhook string, jsonStr []byte) error {
	req, _ := http.NewRequest("POST", webhook, bytes.NewBuffer(jsonStr))
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Errorf("Unable to send message to slack: %v", err)
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode != 200 {
		log.Errorf("Unsuccessful attempt to send message to slack: %s", resp.Status)
	}
	return nil
}

func ParseSQSRecord(record events.SQSMessage) (*events.CloudWatchAlarmSNSPayload, error) {

	var snsEntity events.SNSEntity
	err := json.Unmarshal([]byte(record.Body), &snsEntity)
	unmarshalTypeErr := new(json.UnmarshalTypeError)
	syntaxErr := new(json.SyntaxError)
	if errors.As(err, &unmarshalTypeErr) {
		log.Warn("Skipping event due to unrecognized format for SnsEntity")
		return nil, nil
	} else if errors.As(err, &syntaxErr) {
		log.Warn("Skipping event due to SQS body not being json")
		return nil, nil
	} else if err != nil {
		return nil, err
	}

	var s3Event events.CloudWatchAlarmSNSPayload
	err = json.Unmarshal([]byte(snsEntity.Message), &s3Event)
	if errors.As(err, &unmarshalTypeErr) {
		log.Warn("Skipping event due to unrecognized format for CloudWatchAlarmSNSPayload")
		return nil, nil
	} else if errors.As(err, &syntaxErr) {
		log.Warn("Skipping event due to message in SQS body not being json")
		return nil, nil
	} else if err != nil {
		return nil, err
	}

	return &s3Event, nil
}

func GetParameter(ctx context.Context, keyname string) (string, error) {
	cfg, err := config.LoadDefaultConfig(ctx,
		config.WithRegion("us-east-1"),
		config.WithLogger(logging.Nop{}),
	)
	if err != nil {
		return "", fmt.Errorf("Error creating AWS session: %v", err)
	}

	ssmsvc := ssm.NewFromConfig(cfg)

	withDecryption := true
	result, err := ssmsvc.GetParameter(ctx, &ssm.GetParameterInput{
		Name:           &keyname,
		WithDecryption: &withDecryption,
	})

	if err != nil {
		return "", fmt.Errorf("Error retrieving parameter %s from parameter store: %w", keyname, err)
	}

	val := *result.Parameter.Value

	if val == "" {
		return "", fmt.Errorf("No parameter store value found for %s", keyname)
	}

	return val, nil
}
