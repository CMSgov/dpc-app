package main

import (
	"bufio"
	"bytes"
	"fmt"
	"regexp"
	"strings"
	"time"

	"github.com/google/uuid"
	"github.com/ianlopshire/go-fixedwidth"
	"github.com/pkg/errors"
)

func ParseMetadata(bucket string, filename string) (OptOutFilenameMetadata, error) {
	var metadata OptOutFilenameMetadata
	// P.NGD.DPC.RSP.D240123.T1122001.IN
	// Beneficiary Data Sharing Preferences File sent by 1-800-Medicare: P.NGD.DPC.RSP.Dyymmdd.Thhmmsst.IN
	// Prefix: T = test, P = prod;
	filenameRegexp := regexp.MustCompile(`((P|T)\.NGD)\.DPC\.RSP\.(D\d{6}\.T\d{6})\.IN`)
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

func ParseConsentRecords(metadata *OptOutFilenameMetadata, b []byte) ([]*OptOutRecord, error) {
	var records []*OptOutRecord
	r := bytes.NewReader(b)
	scanner := bufio.NewScanner(r)
	for scanner.Scan() {
		bytes := scanner.Bytes()
		// Do not parse header or footer rows
		if !strings.HasPrefix(string(bytes[:]), "HDR") && !strings.HasPrefix((string(bytes[:])), "TLR") {
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

func ParseRecord(metadata *OptOutFilenameMetadata, b []byte, unmarshaler FileUnmarshaler) (*OptOutRecord, error) {
	var row ResponseFileRow
	if err := unmarshaler(b, &row); err != nil {
		return nil, errors.Wrapf(err, "failed to parse file: %s", metadata.FilePath)
	}

	policyCode, err := ConvertSharingPreference(row.SharingPreference)
	if err != nil {
		return nil, errors.Wrapf(err, "failed to parse file: %s", metadata.FilePath)
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
