package main

import (
	"encoding/json"
	"fmt"
	"os"
	"strconv"
	"testing"

	log "github.com/sirupsen/logrus"

	"github.com/aws/aws-lambda-go/events"
	"github.com/stretchr/testify/assert"
)

func TestParseSQSRecord(t *testing.T) {
	log.SetLevel(log.PanicLevel)

	snsTemplate := "{\"Message\" : %s}"
	alarmBody, _ := os.ReadFile("testdata/cloudWatchEvent.json")

	tests := []struct {
		alarm     bool
		body      string
		messageId string
	}{
		{
			alarm:     true,
			body:      fmt.Sprintf(snsTemplate, strconv.Quote(string(alarmBody))),
			messageId: "Happy Path",
		},
		{
			alarm:     false,
			body:      "foo",
			messageId: "SQS body not json",
		},
		{
			alarm:     false,
			body:      fmt.Sprintf(snsTemplate, "1"),
			messageId: "Bad type for SnsEntity Message",
		},
		{
			alarm:     false,
			body:      fmt.Sprintf(snsTemplate, strconv.Quote("foo")), // sns message is not json
			messageId: "SNS Message is not json",
		},
		{
			alarm:     false,
			body:      fmt.Sprintf(snsTemplate, strconv.Quote("{\"NewStateValue\":1}")),
			messageId: "Bad type for CloudWatchAlarmSNSPayload NewStateValue",
		},
	}
	for _, test := range tests {
		event := events.SQSMessage{
			Body:      test.body,
			MessageId: test.messageId,
		}

		alarmPayload, err := parseSQSRecord(event)
		assert.Nil(t, err)
		if test.alarm {
			assert.NotNil(t, alarmPayload)
			assert.Equal(t, "ALARM", alarmPayload.NewStateValue)
		} else {
			assert.Nil(t, alarmPayload)
		}
	}
}

func TestProcessSQSRecord(t *testing.T) {
	log.SetLevel(log.PanicLevel)
	snsTemplate := "{\"Message\" : %s}"
	alarmBody, _ := os.ReadFile("testdata/cloudWatchEvent.json")
	okBody, _ := os.ReadFile("testdata/cloudWatchOkEvent.json")

	tests := []struct {
		alarm     bool
		body      string
		messageId string
	}{
		{
			alarm:     true,
			body:      fmt.Sprintf(snsTemplate, strconv.Quote(string(alarmBody))),
			messageId: "Happy Path",
		},
		{
			alarm:     false,
			body:      fmt.Sprintf(snsTemplate, strconv.Quote(string(okBody))),
			messageId: "Return to Normal",
		},
		{
			alarm:     false,
			body:      "foo",
			messageId: "SQS body not json",
		},
	}
	for _, test := range tests {
		record := events.SQSMessage{
			Body:      test.body,
			MessageId: test.messageId,
		}
		payload, err := processSQSRecord(record)
		if test.alarm {
			assert.NotNil(t, payload)
			assert.Nil(t, err)
			var slackPayload SlackPayload
			err = json.Unmarshal(payload, &slackPayload)

			assert.Nil(t, err)
			assert.NotNil(t, slackPayload)
			assert.Equal(t, ":anger:", slackPayload.Emoji)
			assert.Equal(t, "ALARM", slackPayload.NewStateValue)
		} else {
			assert.Nil(t, err)
			assert.NotNil(t, payload)
			assert.Equal(t, []byte{}, payload)
		}
	}
}
