package main

import (
	"errors"
	"testing"
	"time"

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
