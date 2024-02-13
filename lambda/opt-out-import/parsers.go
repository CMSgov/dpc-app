package main

import (
	"bufio"
	"bytes"
	"fmt"
	"regexp"
	"strconv"
	"time"

	"github.com/ianlopshire/go-fixedwidth"
	"github.com/pkg/errors"
)

func ParseMetadata(bucket string, filename string) (OptOutFilenameMetadata, error) {
	var metadata OptOutFilenameMetadata
	// Beneficiary Data Sharing Preferences File sent by 1-800-Medicare: P#EFT.ON.ACO.NGD1800.DPRF.Dyymmdd.Thhmmsst
	// Prefix: T = test, P = prod;
	filenameRegexp := regexp.MustCompile(`((P|T)\#EFT)\.ON\.ACO\.NGD1800\.DPRF\.(D\d{6}\.T\d{6})\d`)
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
		if len(bytes) == 459 {
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
	var record OptOutRecord
	if err := unmarshaler(b, &record); err != nil {
		return nil, errors.Wrapf(err, "failed to parse file: %s", metadata.FilePath)
	}
	record.Status = Rejected	// Default to rejected until we successfully process

	var err error 
	if record.EffectiveDt, err = ConvertDt(record.EffectiveDtString); err != nil {
		err = errors.Wrapf(err, "failed to parse the effective date '%s' from file: %s", record.EffectiveDtString, metadata.FilePath)
		return nil, err
	}
	if record.SAMHSAEffectiveDt, err = ConvertDt(record.SAMHSAEffectiveDtString); err != nil {
		err = errors.Wrapf(err, "failed to parse the samhsa effective date '%s' from file: %s", record.SAMHSAEffectiveDtString, metadata.FilePath)
		return nil, err
	}
	lk := record.BeneficiaryLinkKeyString
	if lk == "" {
		lk = "0"
	}
	if record.BeneficiaryLinkKey, err = strconv.Atoi(lk); err != nil {
		err = errors.Wrapf(err, "failed to parse beneficiary link key from file: %s", metadata.FilePath)
		return nil, err
	}

	return &record, nil
}
