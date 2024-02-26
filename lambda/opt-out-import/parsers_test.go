package main

import (
	"errors"
	"fmt"
	"testing"
	"time"

	"github.com/ianlopshire/go-fixedwidth"
	giterr "github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
)

func TestParseMetadata(t *testing.T) {

	// positive
	expTime, _ := time.Parse(time.RFC3339, "2018-11-20T20:13:01Z")
	metadata, _ := ParseMetadata("blah", "T#EFT.ON.ACO.NGD1800.DPRF.D181120.T2013010")
	assert.Equal(t, "T#EFT.ON.ACO.NGD1800.DPRF.D181120.T2013010", metadata.Name)
	assert.Equal(t, expTime.Format("010203040506"), metadata.Timestamp.Format("010203040506"))

	// change the name and timestamp
	expTime, _ = time.Parse(time.RFC3339, "2019-12-20T21:09:42Z")
	metadata, _ = ParseMetadata("blah", "T#EFT.ON.ACO.NGD1800.DPRF.D191220.T2109420")
	assert.Equal(t, "T#EFT.ON.ACO.NGD1800.DPRF.D191220.T2109420", metadata.Name)
	assert.Equal(t, expTime.Format("010203040506"), metadata.Timestamp.Format("010203040506"))
}

func TestParseMetadata_InvalidData(t *testing.T) {

	// invalid file name
	_, err := ParseMetadata("path", "file")
	assert.EqualError(t, err, "invalid filename for file: file")

	_, err = ParseMetadata("/path", "T#EFT.ON.ACO.NGD1800.FRPD.D191220.T1000010")
	assert.EqualError(t, err, "invalid filename for file: T#EFT.ON.ACO.NGD1800.FRPD.D191220.T1000010")

	// invalid date
	_, err = ParseMetadata("/path", "T#EFT.ON.ACO.NGD1800.DPRF.D190117.T9909420")
	assert.EqualError(t, err, "failed to parse date 'D190117.T990942' from file: T#EFT.ON.ACO.NGD1800.DPRF.D190117.T9909420: parsing time \"D190117.T990942\": hour out of range")
}

func TestParseRecord(t *testing.T) {
	// 181120 file
	fileTime, _ := time.Parse(time.RFC3339, "2018-11-20T10:00:00Z")
	line := []byte("5SJ0A00AA001847800005John                          Mitchell                      Doe                                     198203218702 E Fake St.                                        Apt. 63L                                               Region                                                 Las Vegas                               NV423139954M20190618201907011-800TY201907011-800TNT9992WeCare Medical                                                        ")
	metadata := &OptOutFilenameMetadata{
		Timestamp:    fileTime,
		FilePath:     "full-fake-filename",
		Name:         "fake-filename",
		DeliveryDate: time.Now(),
	}

	tests := []struct {
		fileTime    time.Time
		line        []byte
		metadata    *OptOutFilenameMetadata
		unmarshaler FileUnmarshaler
		err         error
	}{
		{
			fileTime:    fileTime,
			line:        line,
			metadata:    metadata,
			unmarshaler: fixedwidth.Unmarshal,
			err:         nil,
		},
		{
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
		if err == nil {
			assert.Equal(t, "5SJ0A00AA00", suppression.MBI)
			assert.Equal(t, Rejected, suppression.Status)
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
			"1000087481 1847800005John                          Mitchell                      Doe                                     198203218702 E Fake St.                                        Apt. 63L                                               Region                                                 Las Vegas                               NV423139954M20190618201913011-800TY201907011-800TNA9999WeCare Medical                                                        		",
			"failed to parse the effective date '20191301' from file"},
		{"1000087481 1847800005John                          Mitchell                      Doe                                     198203218702 E Fake St.                                        Apt. 63L                                               Region                                                 Las Vegas                               NV423139954M20190618201907011-800TY201913011-800TNA9999WeCare Medical                                                        		",
			"failed to parse the samhsa effective date '20191301' from file"},
		{"1000087481 18e7800005John                          Mitchell                      Doe                                     198203218702 E Fake St.                                        Apt. 63L                                               Region                                                 Las Vegas                               NV423139954M20190618201907011-800TY201907011-800TNA9999WeCare Medical                                                        		",
			"failed to parse beneficiary link key from file"},
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
			assert.Contains(t, err.Error(), fmt.Sprintf("%s: %s", tt.expErr, fp))
		})
	}
}
