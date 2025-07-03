package alarm_to_slack

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
	"github.com/pkg/errors"
)

type SlackPayload struct {
	events.CloudWatchAlarmSNSPayload
	Emoji string `json:"Emoji"`
}

func main() {
	lambda.Start(handler)
}

func handler(ctx context.Context, event events.SQSEvent) error {
	log.SetFormatter(&log.JSONFormatter{
		DisableHTMLEscape: true,
		TimestampFormat:   time.RFC3339Nano,
	})

	// This should be set in the platform repo
	webhook := os.Getenv("SLACK_WEBHOOK_URL")

	for _, record := range event.Records {
		messageId := record.MessageId
		payload, err := processSQSRecord(record)
		if err != nil {
			log.WithFields(log.Fields{
				"MessageId": messageId,
			}).Error(fmt.Sprintf("Unable to process SQS Record: %v", err))
		}
		if len(payload) > 0 {
			if webhook != "" {
				sendMessageToSlack(webhook, payload, messageId)
			} else {
				log.WithFields(log.Fields{
					"MessageId": messageId,
				}).Warn("Unable to send to slack as SLACK_WEBHOOK_URL not set")
			}
		}
	}
	return nil
}

func processSQSRecord(record events.SQSMessage) ([]byte, error) {
	alarm, err := parseSQSRecord(record)
	if err != nil || alarm == nil {
		return []byte{}, err
	}

	log.WithFields(log.Fields{
		"MessageId":       record.MessageId,
		"AlarmName":       alarm.AlarmName,
		"NewStateValue":   alarm.NewStateValue,
		"OldStateValue":   alarm.OldStateValue,
		"StateChangeTime": alarm.StateChangeTime,
	}).Info("Received CloudWatch Alarm")

	// return an empty payload if we are returning to an OK state
	if alarm.NewStateValue == "OK" {
		return []byte{}, nil
	}

	slackPayload := SlackPayload{
		CloudWatchAlarmSNSPayload: *alarm,
		Emoji:                     ":anger:",
	}
	return json.Marshal(slackPayload)
}

func parseSQSRecord(record events.SQSMessage) (*events.CloudWatchAlarmSNSPayload, error) {
	messageId := record.MessageId

	var snsEntity events.SNSEntity
	err := json.Unmarshal([]byte(record.Body), &snsEntity)
	if err != nil {
		return nil, handleParseError(err, messageId, "SNSEntity")
	}

	var s3Event events.CloudWatchAlarmSNSPayload
	err = json.Unmarshal([]byte(snsEntity.Message), &s3Event)
	if err != nil {
		return nil, handleParseError(err, messageId, "CloudWatchAlarmSNSPayload")
	}

	return &s3Event, nil
}

func handleParseError(err error, messageId string, entity string) error {
	unmarshalTypeErr := new(json.UnmarshalTypeError)
	syntaxErr := new(json.SyntaxError)

	if errors.As(err, &unmarshalTypeErr) || errors.As(err, &syntaxErr) {
		log.WithFields(log.Fields{
			"MessageId": messageId,
		}).Warn(fmt.Sprintf("Skipping event as cannot unmarshall %s", entity))
		return nil
	}
	return err
}

func sendMessageToSlack(webhook string, jsonStr []byte, messageId string) {
	// Not tested, nothing to return, as testing would just be testing stubs
	req, _ := http.NewRequest("POST", webhook, bytes.NewBuffer(jsonStr))
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.WithFields(log.Fields{
			"MessageId": messageId,
		}).Error(fmt.Sprintf("Unsuccessful attempt to send message to slack: %v", err))
		return
	}
	defer resp.Body.Close()
	if resp.StatusCode == 200 {
		log.WithFields(log.Fields{
			"MessageId": messageId,
		}).Info("Successfully sent message to Slack")
	} else {
		log.WithFields(log.Fields{
			"MessageId": messageId,
		}).Error(fmt.Sprintf("Unsuccessful attempt to send message to slack: %s", resp.Status))
	}
}
