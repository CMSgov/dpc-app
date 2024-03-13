package main

import (
	"database/sql"
	"testing"
	"time"

	"github.com/DATA-DOG/go-sqlmock"
	"github.com/stretchr/testify/assert"
)

func TestGetAttributionData(t *testing.T) {
	patientColumns := []string{"beneficiary_id", "first_name", "last_name", "dob"}
	patientQuery := "SELECT (.+) FROM patients"
	oricreateConnection := createConnection
	db, mock, err := sqlmock.New()
	mockCreateConnection(db)
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	tests := []struct {
		expectedPatientInfos map[string]PatientInfo
		err                  error
		patientQueryResult   *sqlmock.Rows
	}{
		{
			expectedPatientInfos: map[string]PatientInfo{
				"4SH0A00AA00": {
					beneficiary_id: "4SH0A00AA00",
					first_name:     sql.NullString{String: "John", Valid: true},
					last_name:      sql.NullString{String: "Doe", Valid: true},
					dob:            time.Date(2018, 11, 20, 0, 0, 0, 0, time.UTC),
				},
			},
			err:                nil,
			patientQueryResult: sqlmock.NewRows(patientColumns).AddRow("4SH0A00AA00", "John", "Doe", time.Date(2018, 11, 20, 0, 0, 0, 0, time.UTC)),
		},
	}

	for _, test := range tests {
		mock.ExpectQuery(patientQuery).
			WillReturnRows(test.patientQueryResult)
		patientInfos := make(map[string]PatientInfo)
		err = getAttributionData("user", "pass", patientInfos)
		assert.Equal(t, test.expectedPatientInfos, patientInfos)
		assert.Equal(t, test.err, err)
	}
	createConnection = oricreateConnection
}

func TestGetConsentData(t *testing.T) {
	consentColumns := []string{"mbi", "policy_code", "effective_date"}
	consentQuery := "SELECT (.+) FROM consent ORDER BY mbi, effective_date DESC, created_at DESC"
	oricreateConnection := createConnection
	db, mock, err := sqlmock.New()
	mockCreateConnection(db)
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()
	curr_time := time.Now()

	tests := []struct {
		expectedPatientInfos map[string]PatientInfo
		err                  error
		consentQueryResult   *sqlmock.Rows
	}{
		{
			expectedPatientInfos: map[string]PatientInfo{
				"4SH0A00AA00": {
					beneficiary_id: "4SH0A00AA00",
					dob:            time.Date(2018, 11, 20, 0, 0, 0, 0, time.UTC),
					effective_date: curr_time,
					policy_code:    sql.NullString{String: "OPTOUT", Valid: true},
				},
			},
			err:                nil,
			consentQueryResult: sqlmock.NewRows(consentColumns).AddRow("4SH0A00AA00", "OPTOUT", curr_time),
		},
	}

	for _, test := range tests {
		mock.ExpectQuery(consentQuery).
			WillReturnRows(test.consentQueryResult)
		patientInfos := make(map[string]PatientInfo)
		patientInfos["4SH0A00AA00"] = PatientInfo{
			beneficiary_id: "4SH0A00AA00",
			dob:            time.Date(2018, 11, 20, 0, 0, 0, 0, time.UTC),
		}
		err = getConsentData("user", "pass", patientInfos)
		assert.Equal(t, test.expectedPatientInfos, patientInfos)
		assert.Equal(t, test.err, err)
	}
	createConnection = oricreateConnection
}

func mockCreateConnection(db *sql.DB) {
	createConnection = func(dbName string, dbUser string, dbPassword string) (*sql.DB, error) {
		return db, nil
	}
}
