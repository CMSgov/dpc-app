package main

import (
	"bufio"
	"bytes"
	"encoding/json"
	"fmt"
	"os"
	"regexp"
	"strings"
	"time"

	"github.com/aws/aws-lambda-go/events"
	"github.com/google/uuid"
	"github.com/ianlopshire/go-fixedwidth"
	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"
)

func ParseMetadata(bucket string, filename string) (ResponseFileMetadata, error) {
	var metadata ResponseFileMetadata
	// T.NGD.DPC.RSP.D240123.T1122001.IN
	// Beneficiary Data Sharing Preferences File sent by 1-800-Medicare: T.NGD.DPC.RSP.Dyymmdd.Thhmmsst.IN
	// Prefix: T = test, P = prod;
	filenamePrefix := "T"
	if os.Getenv("ENV") == "prod" {
		filenamePrefix = "P"
	}
	regex := fmt.Sprintf(`((%s)\.NGD)\.DPC\.RSP\.(D\d{6}\.T\d{6})\d\.IN`, filenamePrefix)
	filenameRegexp := regexp.MustCompile(regex)
	filenameMatches := filenameRegexp.FindStringSubmatch(filename)
	if len(filenameMatches) < 4 {
		err := fmt.Errorf("invalid filename for file: %s", filename)
		return metadata, err
	}

	filenameDate := filenameMatches[3]
	t, err := time.Parse("D060102.T150405", filenameDate)
	if err != nil || t.IsZero() {
		err = errors.Wrapf(err, "failed to parse date '%s' from file: %s", filenameDate, filename)
		return metadata, err
	}

	metadata.Timestamp = t
	metadata.Name = filenameMatches[0]
	metadata.FilePath = fmt.Sprintf("%s/%s", bucket, filename)

	return metadata, nil
}

func ParseConsentRecords(metadata *ResponseFileMetadata, b []byte) ([]*OptOutRecord, error) {
	var records []*OptOutRecord
	r := bytes.NewReader(b)
	scanner := bufio.NewScanner(r)
	for scanner.Scan() {
		bytes := scanner.Bytes()
		// Do not parse header or footer rows
		if !strings.HasPrefix(string(bytes[:]), "HDR") && !strings.HasPrefix((string(bytes[:])), "TRL") {
			record, err := ParseRecord(metadata, bytes, fixedwidth.Unmarshal)
			if err != nil {
				return records, fmt.Errorf("ParseConsentRecords: %w", err)
			}
			records = append(records, record)
		}
	}
	err := scanner.Err()
	if err != nil {
		return records, fmt.Errorf("ParseConsentRecords: %w", err)
	}
	return records, err
}

// Valid MBIs do not have these letters: BILOSZ
// var validLetters = "AC-HJKMNPQRT-Y"
// var mbiRegex = fmt.Sprintf("(?i)^[1-9][%[1]s][%[1]s0-9]\\d[%[1]s][%[1]s0-9]\\d([%[1]s]){2}(\\d){2}$", validLetters)
var letterPattern = "[AC-HJKMNPQRT-Y]"
var letterOrNumberPattern = "[AC-HJKMNPQRT-Y0-9]"
var matcher = []string { "(?i)",
		"[1-9]",
		letterPattern,
		letterOrNumberPattern,
		"\\d",
		letterPattern,
		letterOrNumberPattern,
		"\\d",
		letterPattern,
		letterPattern,
		"(\\d){2}"}
var mbiRegex = strings.Join(matcher, "")
var mbiPattern = regexp.MustCompile(mbiRegex)

func ParseRecord(metadata *ResponseFileMetadata, b []byte, unmarshaler FileUnmarshaler) (*OptOutRecord, error) {
	var row ResponseFileRow
	if err := unmarshaler(b, &row); err != nil {
		return nil, errors.Wrapf(err, "failed to parse file: %s", metadata.FilePath)
	}

	policyCode, err := ConvertSharingPreference(row.SharingPreference)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to parse file: %s", metadata.FilePath)
	}

	if os.Getenv("ENV") != "prod" {
		mbiMatches := mbiPattern.MatchString(row.MBI)
		if mbiMatches {
			return nil, errors.New("failed to parse file: testfilepath: Valid MBI in non-production environment")
		}
	}

	record := OptOutRecord{
		ID:         uuid.New().String(),
		MBI:        row.MBI,
		PolicyCode: policyCode,
	}
	return &record, nil
}

func ConvertSharingPreference(pref string) (string, error) {
	if pref == "Y" {
		return "OPTIN", nil
	} else if pref == "N" {
		return "OPTOUT", nil
	} else {
		return "", errors.New(fmt.Sprintf("Unexpected value %s for sharing preference", pref))
	}
}

// TODO: Iterate over records
func ParseSQSEvent(event events.SQSEvent) (*events.S3Event, error) {
	var snsEntity events.SNSEntity
	err := json.Unmarshal([]byte(event.Records[0].Body), &snsEntity)

	unmarshalTypeErr := new(json.UnmarshalTypeError)
	if errors.As(err, &unmarshalTypeErr) {
		log.Warn("Skipping event due to unrecognized format for SNS")
		return nil, nil
	} else if err != nil {
		return nil, err
	}

	var s3Event events.S3Event
	err = json.Unmarshal([]byte(snsEntity.Message), &s3Event)

	unmarshalTypeErr = new(json.UnmarshalTypeError)
	if errors.As(err, &unmarshalTypeErr) {
		log.Warn("Skipping event due to unrecognized format for S3")
		return nil, nil
	} else if err != nil {
		return nil, err
	}

	return &s3Event, nil
}
