package main

import (
	"bytes"
	"bufio"
	"database/sql"
	"fmt"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/stretchr/testify/assert"
	"opt-out-beneficiary-data-lambda/dpcaws"
)

func TestIntegrationGenerateRequestFile(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test.")
	}
	oriGetSecret := getSecret
	oriGetSecrets := getSecrets
	oriCreateConnection := createConnection
	oriGetAttributionData := getAttributionData
	oriGetConsentData := getConsentData

	tests := []struct {
		err      error
		mockFunc func()
	}{
		{
			err: nil,
			mockFunc: func() {
				getSecret = func(s *session.Session, keyname string) (string, error) {
					return "fake_arn", nil
				}

				getSecrets = func(s *session.Session, keynames []*string) (map[string]string, error) {
					return map[string]string{
						"/dpc/dev/attribution/db_user_dpc_attribution": "db_user_dpc_attribution",
						"/dpc/dev/attribution/db_pass_dpc_attribution": "db_pass_dpc_attribution",
						"/dpc/dev/consent/db_user_dpc_consent":         "db_user_dpc_consent",
						"/dpc/dev/consent/db_pass_dpc_consent":         "db_pass_dpc_consent",
					}, nil
				}

				getAttributionData = func(dbUser string, dbPassword string, patientInfos map[string]PatientInfo) error {
					return nil
				}

				getConsentData = func(dbUser string, dbPassword string, patientInfos map[string]PatientInfo) error {
					patientInfos["optout"] = PatientInfo{
						beneficiary_id: "optout",
						first_name:     sql.NullString{String: "fname", Valid: true},
						last_name:      sql.NullString{String: "lname", Valid: true},
						dob:            time.Date(1967, 01, 10, 0, 0, 0, 0, time.UTC),
						effective_date: time.Now(),
						policy_code:    sql.NullString{String: "OPTOUT", Valid: true},
					}
					patientInfos["optin"] = PatientInfo{
						beneficiary_id: "optin",
						first_name:     sql.NullString{String: "fname", Valid: true},
						last_name:      sql.NullString{String: "lname", Valid: true},
						dob:            time.Date(1968, 01, 10, 0, 0, 0, 0, time.UTC),
						effective_date: time.Now(),
						policy_code:    sql.NullString{String: "OPTIN", Valid: true},
					}
					patientInfos["noconsent"] = PatientInfo{
						beneficiary_id: "noconsent",
						first_name:     sql.NullString{String: "fname", Valid: true},
						last_name:      sql.NullString{String: "lname", Valid: true},
						dob:            time.Date(1969, 01, 10, 0, 0, 0, 0, time.UTC),
						effective_date: time.Now(),
						policy_code:    sql.NullString{String: "", Valid: false},
					}

					return nil
				}
			},
		},
	}

	session, sessionErr := getAwsSession()
	assert.Nil(t, sessionErr)
	for _, test := range tests {
		test.mockFunc()
		filename, err := generateRequestFile()
		assert.NotEmpty(t, filename)
		assert.Nil(t, err)

		b, downloadErr := dpcaws.DownloadFileFromS3(session, os.Getenv("S3_UPLOAD_BUCKET"), fmt.Sprintf("%s/%s", os.Getenv("S3_UPLOAD_PATH"), filename))
		assert.Nil(t, downloadErr)
		r := bytes.NewReader(b)
		scanner := bufio.NewScanner(r)
		ctr := 0
		for scanner.Scan() {
			ctr += 1
		}
		assert.Equal(t, 5, ctr)
	}

	getSecret = oriGetSecret
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
		buff, _ := formatFileData("test-file", test.patientInfos)
		body := buff.String()
		lines := strings.Split(string(body), "\n")
		assert.Equal(t, len(test.expect), len(lines))
		assert.ElementsMatch(t, test.expect, lines)
	}
}

func TestGetAwsSession(t *testing.T) {
	tests := []struct {
		expect          *session.Session
		err             error
		newSession      func(roleArn string) (*session.Session, error)
		newLocalSession func(endPoint string) (*session.Session, error)
		setEnvironment  func()
		isTesting       bool
	}{
		{
			// Happy path, testing
			expect:          nil,
			err:             nil,
			newSession:      func(roleArn string) (*session.Session, error) { return nil, nil },
			newLocalSession: func(endPoint string) (*session.Session, error) { return nil, nil },
			setEnvironment: func() {
				t.Setenv("LOCAL_STACK_ENDPOINT", "endpoint")
			},
			isTesting: true,
		},
		{
			// LOCAL_STACK_ENDPOINT not set, testing
			expect:          nil,
			err:             fmt.Errorf("LOCAL_STACK_ENDPOINT env variable not defined"),
			newSession:      func(roleArn string) (*session.Session, error) { return nil, nil },
			newLocalSession: func(endPoint string) (*session.Session, error) { return nil, nil },
			setEnvironment: func() {
				os.Unsetenv("LOCAL_STACK_ENDPOINT")
			},
			isTesting: true,
		},
		{
			// Happy path, not testing
			expect:          nil,
			err:             nil,
			newSession:      func(roleArn string) (*session.Session, error) { return nil, nil },
			newLocalSession: func(endPoint string) (*session.Session, error) { return nil, nil },
			setEnvironment:  func() {},
			isTesting:       false,
		},
	}

	for _, test := range tests {
		newSession = test.newSession
		newLocalSession = test.newLocalSession
		isTesting = test.isTesting

		test.setEnvironment()
		s, err := getAwsSession()

		assert.Equal(t, test.expect, s)
		assert.Equal(t, test.err, err)
	}
}

func TestGenerateRequestFileName_Test(t *testing.T) {
	now, _ := time.Parse("2006-01-02 15:04:05", "2010-01-01 12:00:00")
	fileName := generateRequestFileName(now)
	assert.Equal(t, "T#EFT.ON.DPC.NGD.REQ.D100101.T1200000", fileName)
}

func TestGenerateRequestFileName_Prod(t *testing.T) {
	testEnv := os.Getenv("ENV")

	os.Setenv("ENV", "prod")
	defer os.Setenv("ENV", testEnv)

	now, _ := time.Parse("2006-01-02 15:04:05", "2010-01-01 12:00:00")
	fileName := generateRequestFileName(now)
	assert.Equal(t, "P#EFT.ON.DPC.NGD.REQ.D100101.T1200000", fileName)
}
