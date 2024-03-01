package main

import (
	"database/sql/driver"
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/DATA-DOG/go-sqlmock"
	log "github.com/sirupsen/logrus"
	"github.com/stretchr/testify/assert"
)

var envDbuser = os.Getenv("DB_USER_DPC_CONSENT")
var envDbpassword = os.Getenv("DB_PASS_DPC_CONSENT")

type AnyString struct{}

func (a AnyString) Match(v driver.Value) bool {
	_, ok := v.(string)
	return ok
}

func TestGetConsentDbSecrets(t *testing.T) {
	tests := []struct {
		name       string
		dbuser     string
		dbpassword string
		expect     map[string]string
		err        error
	}{
		{
			name:       "happy path",
			dbuser:     "dpc/local/consent/db_user_dpc_consent",
			dbpassword: "dpc/local/consent/db_password_dpc_consent",
			expect: map[string]string{
				"dpc/local/consent/db_user_dpc_consent":     envDbuser,
				"dpc/local/consent/db_password_dpc_consent": envDbpassword,
			},
			err: nil,
		},
	}

	for _, test := range tests {
		fmt.Printf("~~~ %s test\n", test.name)
		secretsInfo, err := getConsentDbSecrets(test.dbuser, test.dbpassword)
		assert.Equal(t, test.expect, secretsInfo)
		if test.err != nil {
			assert.ErrorContains(t, err, test.err.Error())
		}
	}
}

func TestInsertOptOutMetadata(t *testing.T) {
	tests := []struct {
		name     string
		bucket   string
		filename string
		expect   bool
		err      error
	}{
		{
			name:     "happy path",
			bucket:   "demo-bucket",
			filename: "T#EFT.ON.ACO.NGD1800.DPRF.D181120.T1000009",
			expect:   true,
			err:      nil,
		},
	}

	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	for _, test := range tests {
		fmt.Printf("~~~ %s test\n", test.name)

		metadata, err := ParseMetadata(test.bucket, test.filename)
		if err != nil {
			t.Errorf("Error when parsing opt out metadata %s", err)
		}

		timestampValue := time.Date(2018, 11, 20, 0, 0, 0, 0, time.UTC)
		rows := []string{"id", "name", "timestamp", "import_status"}
		mock.ExpectQuery("INSERT INTO opt_out_file").
			WithArgs(AnyString{}, "T#EFT.ON.ACO.NGD1800.DPRF.D181120.T1000009", "2018-11-20").
			WillReturnRows(sqlmock.NewRows(rows).AddRow("(.*)", "T#EFT.ON.ACO.NGD1800.DPRF.D181120.T1000009", timestampValue, "In-Progress"))

		entity, err := insertOptOutMetadata(db, &metadata)
		if err != nil {
			t.Error(err)
		}
		assert.Equal(t, test.expect, entity.id != "")
		if test.err != nil {
			assert.ErrorContains(t, err, test.err.Error())
		}
	}

}

func TestInsertConsentRecords(t *testing.T) {
	tests := []struct {
		name          string
		bucket        string
		filename      string
		expect        bool
		consentStatus string
		err           error
	}{
		{
			name:          "happy path",
			bucket:        "demo-bucket",
			filename:      "T#EFT.ON.ACO.NGD1800.DPRF.D181120.T1000009",
			expect:        true,
			consentStatus: Accepted,
			err:           nil,
		},
	}

	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	for _, test := range tests {
		fmt.Printf("~~~ %s test\n", test.name)

		f, err := os.ReadFile(fmt.Sprintf("synthetic_test_data/%s", test.filename))
		if err != nil {
			fmt.Printf("unable to read file: %v", err)
		}

		metadata, err := ParseMetadata(test.bucket, test.filename)
		if err != nil {
			t.Errorf("Error when parsing opt out metadata %s", err)
		}
		metadata.FileID = "test_id"
		consents, err := ParseConsentRecords(&metadata, f)
		if err != nil {
			t.Errorf("Error when parsing consent records %s", err)
		}
		rows := []string{"id", "mbi", "effective_date", "opt_out_file_id"}
		mock.ExpectQuery("INSERT INTO consent").
			WillReturnRows(sqlmock.NewRows(rows).
				AddRow("(.*)", "5SJ0A00AA00", time.Date(2019, 07, 01, 0, 0, 0, 0, time.UTC), "test_id").
				AddRow("(.*)", "4SF6G00AA00", time.Date(2019, 07, 29, 0, 0, 0, 0, time.UTC), "test_id").
				AddRow("(.*)", "4SH0A00AA00", time.Date(0001, 01, 01, 0, 0, 0, 0, time.UTC), "test_id").
				AddRow("(.*)", "8SG0A00AA00", time.Date(2019, 07, 19, 0, 0, 0, 0, time.UTC), "test_id"))

		rows = []string{"id", "import_status"}
		mock.ExpectQuery("UPDATE opt_out_file").
			WithArgs(ImportComplete, metadata.FileID).
			WillReturnRows(sqlmock.NewRows(rows).AddRow(metadata.FileID, ImportComplete))
		results, err := insertConsentRecords(db, "test_id", consents)
		log.Printf("results: %d", len(results))
		if err != nil {
			t.Error(err)
		}
		assert.Equal(t, test.expect, len(results) == 4)

		// All created records show processed = true
		for _, result := range results {
			assert.Equal(t, test.consentStatus, result.Status)
		}
		// All passed in records show processed = true
		for _, consent := range consents {
			assert.Equal(t, test.consentStatus, consent.Status)
		}

		if test.err != nil {
			assert.ErrorContains(t, err, test.err.Error())
		}

	}
}

func TestInsertConsentRecords_DatabaseError(t *testing.T) {
	test := struct {
		name          string
		bucket        string
		filename      string
		consentStatus string
	}{
		name:          "database error",
		bucket:        "demo-bucket",
		filename:      "T#EFT.ON.ACO.NGD1800.DPRF.D181120.T1000009",
		consentStatus: Rejected,
	}

	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	fmt.Printf("~~~ %s test\n", test.name)
	f, err := os.ReadFile(fmt.Sprintf("synthetic_test_data/%s", test.filename))
	if err != nil {
		fmt.Printf("unable to read file: %v", err)
	}

	metadata, err := ParseMetadata(test.bucket, test.filename)
	if err != nil {
		t.Errorf("Error when parsing opt out metadata %s", err)
	}
	metadata.FileID = "test_id"
	consents, err := ParseConsentRecords(&metadata, f)
	if err != nil {
		t.Errorf("Error when parsing consent records %s", err)
	}

	mock.ExpectQuery("INSERT INTO consent").
		WillReturnError(fmt.Errorf("mock database error"))
	rows := []string{"id", "import_status"}
	mock.ExpectQuery("UPDATE opt_out_file").
		WithArgs(ImportFail, metadata.FileID).
		WillReturnRows(sqlmock.NewRows(rows).AddRow(metadata.FileID, ImportFail))

	response, err := insertConsentRecords(db, metadata.FileID, consents)
	assert.Empty(t, response)
	for _, consent := range consents {
		assert.Equal(t, test.consentStatus, consent.Status)
	}

	for _, res := range response {
		fmt.Println(fmt.Printf("record %s: %s", res.ID, res.SourceCode))
	}
	assert.EqualError(t, err, "insertConsentRecords: failed to insert to consent table: mock database error")
	assert.Equal(t, 0, len(response))
}

func TestUpdateOptOutFileImportStatus(t *testing.T) {
	tests := []struct {
		importStatus string
		err          error
	}{
		{
			importStatus: ImportComplete,
			err:          nil,
		},
		{
			importStatus: ImportFail,
			err:          nil,
		},
	}

	db, mock, err := sqlmock.New()
	if err != nil {
		t.Errorf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	for _, test := range tests {
		id := "test-id"
		updated_at := "2024-01-23 16:36:00.616721+00"
		rows := []string{"id", "import_status", "updated_at"}
		mock.ExpectQuery(`UPDATE opt_out_file`).
			WithArgs(test.importStatus, id).
			WillReturnRows(sqlmock.NewRows(rows).AddRow(id, test.importStatus, updated_at))
		err := updateOptOutFileImportStatus(db, id, test.importStatus)
		assert.Equal(t, test.err, err)
	}
}
