package main

import (
	"errors"
	"testing"
	"time"

	"github.com/aws/aws-lambda-go/events"
	"github.com/ianlopshire/go-fixedwidth"
	giterr "github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
)

func TestParseMetadata(t *testing.T) {

	// positive
	expTime, _ := time.Parse(time.RFC3339, "2024-01-23T11:22:00Z")
	metadata, _ := ParseMetadata("blah", "P.NGD.DPC.RSP.D240123.T1122001.IN")
	assert.Equal(t, "P.NGD.DPC.RSP.D240123.T1122001.IN", metadata.Name)
	assert.Equal(t, expTime.Format("D060102.T150405"), metadata.Timestamp.Format("D060102.T150405"))

	// change the name and timestamp
	expTime, _ = time.Parse(time.RFC3339, "2019-01-23T11:22:00Z")
	metadata, _ = ParseMetadata("blah", "P.NGD.DPC.RSP.D190123.T1122001.IN")
	assert.Equal(t, "P.NGD.DPC.RSP.D190123.T1122001.IN", metadata.Name)
	assert.Equal(t, expTime.Format("D060102.T150405"), metadata.Timestamp.Format("D060102.T150405"))
}

func TestParseMetadata_InvalidData(t *testing.T) {

	// invalid file name
	_, err := ParseMetadata("path", "file")
	assert.EqualError(t, err, "invalid filename for file: file")

	_, err = ParseMetadata("/path", "P.NGD.DPC.RSP.D240123.T1122001")
	assert.EqualError(t, err, "invalid filename for file: P.NGD.DPC.RSP.D240123.T1122001")

	// invalid date
	_, err = ParseMetadata("/path", "P.NGD.DPC.RSP.D190117.T9909420.IN")
	assert.EqualError(t, err, "failed to parse date 'D190117.T990942' from file: P.NGD.DPC.RSP.D190117.T9909420.IN: parsing time \"D190117.T990942\": hour out of range")
}

func TestParseRecord(t *testing.T) {
	// 181120 file
	fileTime, _ := time.Parse(time.RFC3339, "2018-11-20T10:00:00Z")
	line := []byte("1SJ0A00AA00N")
	metadata := &OptOutFilenameMetadata{
		Timestamp:    fileTime,
		FilePath:     "full-fake-filename",
		Name:         "fake-filename",
		DeliveryDate: time.Now(),
	}

	tests := []struct {
		number      int
		fileTime    time.Time
		line        []byte
		metadata    *OptOutFilenameMetadata
		unmarshaler FileUnmarshaler
		err         error
	}{
		{
			number:      1,
			fileTime:    fileTime,
			line:        []byte("1SJ0A00AA00N"),
			metadata:    metadata,
			unmarshaler: fixedwidth.Unmarshal,
			err:         nil,
		},
		{
			number:      2,
			fileTime:    fileTime,
			line:        []byte("1SJ0A00AA00Y"),
			metadata:    metadata,
			unmarshaler: fixedwidth.Unmarshal,
			err:         nil,
		},
		{
			number:   3,
			fileTime: fileTime,
			line:     line,
			metadata: metadata,
			unmarshaler: func(data []byte, v interface{}) error {
				return errors.New("Unmarshaling failed")
			},
			err: errors.New("Unmarshaling failed"),
		},
	}

	for _, test := range tests {
		suppression, err := ParseRecord(test.metadata, test.line, test.unmarshaler)
		if test.number == 1 {
			assert.Equal(t, "1SJ0A00AA00", suppression.MBI)
			assert.Equal(t, "OPTOUT", suppression.PolicyCode)
		} else if test.number == 2 {
			assert.Equal(t, "1SJ0A00AA00", suppression.MBI)
			assert.Equal(t, "OPTIN", suppression.PolicyCode)
		} else {
			assert.Equal(t, test.err, giterr.Cause(err))
		}
	}
}

func TestParseRecord_InvalidData(t *testing.T) {
	fp := "testfilepath"

	tests := []struct {
		line   string
		expErr string
	}{
		{
			"1SJ0A00AA00Q",
			"failed to parse file: testfilepath: Unexpected value Q for sharing preference",
		},
	}

	for _, tt := range tests {
		t.Run(tt.line, func(t *testing.T) {
			metadata := &OptOutFilenameMetadata{
				Timestamp:    time.Now(),
				FilePath:     fp,
				Name:         tt.line,
				DeliveryDate: time.Now(),
			}
			suppression, err := ParseRecord(metadata, []byte(tt.line), fixedwidth.Unmarshal)
			assert.Nil(t, suppression)
			assert.NotNil(t, err)
			assert.Contains(t, err.Error(), tt.expErr)
		})
	}
}

func TestParseSQSEvent(t *testing.T) {
	body := "{\n  \"Type\" : \"Notification\",\n  \"MessageId\" : \"27e4306f-db21-52e8-ac8b-6e06896db643\",\n  \"TopicArn\" : \"arn:aws:sns:us-east-1:577373831711:bfd-test-eft-inbound-received-s3-dpc\",\n  \"Subject\" : \"Amazon S3 Notification\",\n  \"Message\" : \"{\\\"Records\\\":[{\\\"eventVersion\\\":\\\"2.1\\\",\\\"eventSource\\\":\\\"aws:s3\\\",\\\"awsRegion\\\":\\\"us-east-1\\\",\\\"eventTime\\\":\\\"2024-03-11T18:40:11.978Z\\\",\\\"eventName\\\":\\\"ObjectCreated:Put\\\",\\\"userIdentity\\\":{\\\"principalId\\\":\\\"AWS:AROAYM3RJQIP6E7GYLGEM:GitHubActions\\\"},\\\"requestParameters\\\":{\\\"sourceIPAddress\\\":\\\"52.159.142.207\\\"},\\\"responseElements\\\":{\\\"x-amz-request-id\\\":\\\"1FBF7M97BMGBA51P\\\",\\\"x-amz-id-2\\\":\\\"HTuKjp1ErjzUZRXSrcwqrKGd+R8pZwM/Xe7ozkCqJguFgSJw8MyIuW8+AE0SIxTDffnQs9wahu4+BE6IGIWKBjgB6JMUaJSYNVdUwMxp2RQ=\\\"},\\\"s3\\\":{\\\"s3SchemaVersion\\\":\\\"1.0\\\",\\\"configurationId\\\":\\\"bfd-test-eft-inbound-received-s3-dpc\\\",\\\"bucket\\\":{\\\"name\\\":\\\"bfd-test-eft\\\",\\\"ownerIdentity\\\":{\\\"principalId\\\":\\\"A5VBSMJFI0FCE\\\"},\\\"arn\\\":\\\"arn:aws:s3:::bfd-test-eft\\\"},\\\"object\\\":{\\\"key\\\":\\\"bfdeft01/dpc/in/P.NGD.DPC.RSP.D240311.T1840071.IN\\\",\\\"size\\\":148,\\\"eTag\\\":\\\"ab963837c0a2bb70c7f0d3aa886bf24c\\\",\\\"sequencer\\\":\\\"0065EF500BCCA7C4C6\\\"}}}]}\",\n  \"Timestamp\" : \"2024-03-11T18:40:12.630Z\",\n  \"SignatureVersion\" : \"1\",\n  \"Signature\" : \"W2xExh3H6n7j3U9RFliPV4gaG3oLAps2ilrpStqQaAkJ5ebf4+/gB2NKk9LH6e7rlX4+JAFlAwVGVNX+fdKNbBVKPpt+uyk+Ng586yQxVPoAFdbnnVu/KnyKnZ42iilNSR2vridid/LQBGkRWEqpBhYbtg/Ny/rZWD6PzqW0RktiNLscgat/i/PwOY6bmhz9fmd2NysRqh+BptrE4NZtEc6YxT1AOLswnj8KVcSlv0sEuD+/71Qrd69XKK+62yoXnH65+adLrejEVFQcv8MYVGsexvkhesQjWooTu6Kw2y/b/atp250d1yJPLR+UTuYAII0Z1rcmCIwpvB/wUuzBBw==\",\n  \"SigningCertURL\" : \"https://sns.us-east-1.amazonaws.com/SimpleNotificationService-60eadc530605d63b8e62a523676ef735.pem\",\n  \"UnsubscribeURL\" : \"https://sns.us-east-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:577373831711:bfd-test-eft-inbound-received-s3-dpc:5a305355-b238-4c5b-b1fb-e987474b09e4\"\n}"
	event := events.SQSEvent{
		Records: []events.SQSMessage{{Body: body}},
	}

	s3Event, err := ParseSQSEvent(event)
	assert.Nil(t, err)
	println(s3Event.Records[0].S3.Bucket.Name)
	assert.NotNil(t, s3Event)
}
