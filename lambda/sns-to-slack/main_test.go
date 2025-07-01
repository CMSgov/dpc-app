package main

import (
	"fmt"
	"os"
	"strconv"
	"testing"

	"github.com/aws/aws-lambda-go/events"
	"github.com/stretchr/testify/assert"
)

func TestParseSQSRecord(t *testing.T) {

	snsTemplate := "{\"Message\" : %s}"
	alarmBody, _ := os.ReadFile("testdata/cloudWatchEvent.json")
	
	tests := []struct {
		alarm bool
		body  string
	}{
		{
			alarm: true,
			body: fmt.Sprintf(snsTemplate, strconv.Quote(string(alarmBody))),
		},
		{
			alarm: false,
			body: "foo", // sqs body is not json
		},
		{
			alarm: false,
			body: fmt.Sprintf(snsTemplate, "1"), // bad type for SnsEntity Message
		},
		{
			alarm: false,
			body: fmt.Sprintf(snsTemplate, strconv.Quote("foo")), // sns message is not json
		},
		{
			alarm: false,
			body: fmt.Sprintf(snsTemplate, strconv.Quote("{\"NewStateValue\":1}")), // bad type for CloudWatchAlarmSNSPayload NewStateValue
		},
		
	}
	for _, test := range tests {
		event := events.SQSMessage{
			Body: test.body,
		}
		
		alarmPayload, err := ParseSQSRecord(event)
		assert.Nil(t, err)
		if test.alarm {
			assert.NotNil(t, alarmPayload)
			assert.Equal(t, "ALARM", alarmPayload.NewStateValue)
		} else {
			assert.Nil(t, alarmPayload)
		}
	}
}
