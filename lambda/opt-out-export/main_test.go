package main

import (
	"context"
	"database/sql"
	"io/ioutil"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestHandler(t *testing.T) {
	tests := []struct {
		event  Event
		expect string
		err    error
	}{
		{
			event:  Event{date: "2023-08-22"},
			expect: "",
		},
	}

	for _, test := range tests {
		response, _ := handler(context.Background(), test.event)
		assert.Equal(t, test.expect, response)
	}
}

func TestGenerateBeneAlignmentFile(t *testing.T) {
	oriGetSecrets := getSecrets
	oriCreateConnection := createConnection
	oriGetAttributionData := getAttributionData
	oriGetConsentData := getConsentData

	tests := []struct {
		expect   string
		err      error
		mockFunc func()
	}{
		{
			expect: "bene_alignment_file.txt",
			err:    nil,
			mockFunc: func() {
				getSecrets = func(keynames []*string) (map[string]string, error) {
					return map[string]string{
						"/dpc/dev/attribution/db_user_dpc_attribution": "db_user_dpc_attribution",
						"/dpc/dev/attribution/db_pass_dpc_attribution": "db_pass_dpc_attribution",
						"/dpc/dev/consent/db_user_dpc_consent":         "db_user_dpc_consent",
						"/dpc/dev/consent/db_pass_dpc_consent":         "db_pass_dpc_consent",
					}, nil
				}

				getAttributionData = func(dbUser string, dbPassword string, patientInfos map[string]PatientInfo) error {
					patientInfos["test_id"] = PatientInfo{
						beneficiary_id: "test_id",
						first_name:     sql.NullString{String: "fname", Valid: true},
						last_name:      sql.NullString{String: "lname", Valid: true},
						dob:            time.Now(),
					}

					return nil
				}

				getConsentData = func(dbUser string, dbPassword string, patientInfos map[string]PatientInfo) error {
					patientInfos["test_id"] = PatientInfo{
						beneficiary_id: "test_id",
						first_name:     sql.NullString{String: "fname", Valid: true},
						last_name:      sql.NullString{String: "lname", Valid: true},
						dob:            time.Now(),
						effective_date: time.Now(),
						policy_code:    sql.NullString{String: "OPTOUT", Valid: true},
					}
					return nil
				}
			},
		},
	}

	for _, test := range tests {
		test.mockFunc()
		filename, _ := generateBeneAlignmentFile()
		assert.Equal(t, test.expect, filename)
	}

	getSecrets = oriGetSecrets
	createConnection = oriCreateConnection
	getAttributionData = oriGetAttributionData
	getConsentData = oriGetConsentData
}

func TestFormatFileData(t *testing.T) {
	curr_date := time.Now().Format("20060102")
	tests := []struct {
		testName     string
		patientInfos map[string]PatientInfo
		expect       []string
		err          error
	}{
		{
			testName: "Test multiple records with optin and optout status",
			patientInfos: map[string]PatientInfo{
				"4SH0A00AA00": {
					beneficiary_id: "4SH0A00AA00",
					first_name:     sql.NullString{String: "Jacob", Valid: true},
					last_name:      sql.NullString{String: "Brown", Valid: true},
					dob:            time.Date(2018, 11, 20, 0, 0, 0, 0, time.UTC),
					effective_date: time.Now(),
					policy_code:    sql.NullString{String: "OPTOUT", Valid: true},
				},
				"8SG0A00AA00": {
					beneficiary_id: "8SG0A00AA00",
					first_name:     sql.NullString{String: "Janice", Valid: true},
					last_name:      sql.NullString{String: "J", Valid: true},
					dob:            time.Date(2008, 12, 20, 0, 0, 0, 0, time.UTC),
					effective_date: time.Now(),
					policy_code:    sql.NullString{String: "OPTIN", Valid: true},
				},
			},
			expect: []string{"HDR_BENEDATAREQ" + curr_date,
				"4SH0A00AA00Jacob                         Brown                                   20181120" + curr_date + "N",
				"8SG0A00AA00Janice                        J                                       20081220" + curr_date + "Y",
				"TRL_BENEDATAREQ" + curr_date + "0000000002"},
			err: nil,
		},
		{
			testName: "Test record with data from only patients table",
			patientInfos: map[string]PatientInfo{
				"6SJ0A00AW00": {
					beneficiary_id: "6SJ0A00AW00",
					first_name:     sql.NullString{String: "John", Valid: false},
					last_name:      sql.NullString{String: "Smith", Valid: true},
					dob:            time.Date(2008, 12, 20, 0, 0, 0, 0, time.UTC),
				},
			},
			expect: []string{"HDR_BENEDATAREQ" + curr_date,
				"6SJ0A00AW00John                          Smith                                   20081220         ",
				"TRL_BENEDATAREQ" + curr_date + "0000000001"},
			err: nil,
		},
	}

	for _, test := range tests {
		filename, _ := formatFileData(test.patientInfos)
		body, _ := ioutil.ReadFile(filename)
		lines := strings.Split(string(body), "\n")
		assert.Equal(t, len(test.expect), len(lines))
		assert.ElementsMatch(t, test.expect, lines)
		os.Remove(filename)
	}
}
