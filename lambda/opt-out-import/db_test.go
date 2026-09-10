package main

import (
	"context"
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

func TestInsertResponseFileMetadata(t *testing.T) {
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
			filename: "T.NGD.DPC.RSP.D240123.T1122001.IN",
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

		timestampValue := time.Date(2024, 01, 23, 0, 0, 0, 0, time.UTC)
		rows := []string{"id", "name", "timestamp", "import_status"}
		mock.ExpectQuery("INSERT INTO opt_out_file").
			WithArgs(AnyString{}, "T.NGD.DPC.RSP.D240123.T1122001.IN", "2024-01-23").
			WillReturnRows(sqlmock.NewRows(rows).AddRow("(.*)", "T.NGD.DPC.RSP.D240123.T1122001.IN", timestampValue, "In-Progress"))

		entity, err := insertResponseFileMetadata(db, &metadata)
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
			filename:      "T.NGD.DPC.RSP.D240123.T1122001.IN",
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

		fileId := "test_id"
		metadata.FileID = fileId
		consents, err := ParseConsentRecords(&metadata, f)
		if err != nil {
			t.Errorf("Error when parsing consent records %s", err)
		}
		rows := []string{"id", "mbi", "effective_date", "policy_code", "opt_out_file_id"}
		mock.ExpectQuery("INSERT INTO consent").
			WillReturnRows(sqlmock.NewRows(rows).
				AddRow("(.*)", "5SJ0A00AA00", time.Date(2019, 07, 01, 0, 0, 0, 0, time.UTC), "OPTOUT", fileId).
				AddRow("(.*)", "4SF6G00AA00", time.Date(2019, 07, 29, 0, 0, 0, 0, time.UTC), "OPTOUT", fileId).
				AddRow("(.*)", "4SH0A00AA00", time.Date(0001, 01, 01, 0, 0, 0, 0, time.UTC), "OPTOUT", fileId).
				AddRow("(.*)", "8SG0A00AA00", time.Date(2019, 07, 19, 0, 0, 0, 0, time.UTC), "OPTOUT", fileId))

		rows = []string{"id", "import_status"}
		mock.ExpectQuery("UPDATE opt_out_file").
			WithArgs(ImportComplete, metadata.FileID).
			WillReturnRows(sqlmock.NewRows(rows).AddRow(metadata.FileID, ImportComplete))
		results, err := insertConsentRecords(db, fileId, consents)
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
		name:     "database error",
		bucket:   "demo-bucket",
		filename: "T.NGD.DPC.RSP.D240123.T1122001.IN",
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
	consentRecords, err := ParseConsentRecords(&metadata, f)
	if err != nil {
		t.Errorf("Error when parsing consent records %s", err)
	}

	mock.ExpectQuery("INSERT INTO consent").
		WillReturnError(fmt.Errorf("mock database error"))
	rows := []string{"id", "import_status"}
	mock.ExpectQuery("UPDATE opt_out_file").
		WithArgs(ImportFail, metadata.FileID).
		WillReturnRows(sqlmock.NewRows(rows).AddRow(metadata.FileID, ImportFail))

	response, err := insertConsentRecords(db, metadata.FileID, consentRecords)
	assert.Empty(t, response)

	for _, res := range response {
		fmt.Println(fmt.Printf("record %s", res.ID))
	}
	assert.EqualError(t, err, "insertConsentRecords: failed to insert to consent table: mock database error")
	assert.Equal(t, 0, len(response))
}

func TestInsertConsentRecordsEmptyFile(t *testing.T) {
	tests := []struct {
		name          string
		bucket        string
		filename      string
		expect        bool
		consentStatus string
		err           error
	}{
		{
			name:          "empty file",
			bucket:        "demo-bucket",
			filename:      "T.NGD.DPC.RSP.D010424.T1122001.IN", // File has header and footer, but no data rows
			expect:        true,
			consentStatus: Accepted,
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
		assert.Empty(t, consents)

		rows := []string{"id", "import_status"}
		mock.ExpectQuery("UPDATE opt_out_file").
			WithArgs(ImportComplete, metadata.FileID).
			WillReturnRows(sqlmock.NewRows(rows).AddRow(metadata.FileID, ImportComplete))
		results, err := insertConsentRecords(db, "test_id", consents)
		if err != nil {
			t.Error(err)
		}
		assert.Empty(t, results)
	}
}

func TestInsertConsentRecordsEmptyRecordsDirectCall(t *testing.T) {
	tests := []struct {
		name          string
		bucket        string
		filename      string
		expect        bool
		consentStatus string
		err           error
	}{
		{
			name: "empty array",
		},
	}

	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	for _, test := range tests {
		fmt.Printf("~~~ %s test\n", test.name)

		fileId := "test_id"

		rows := []string{"id", "import_status"}
		mock.ExpectQuery("UPDATE opt_out_file").
			WithArgs(ImportComplete, fileId).
			WillReturnRows(sqlmock.NewRows(rows).AddRow(fileId, ImportComplete))

		var consents []*OptOutRecord
		results, err := insertConsentRecords(db, "test_id", consents)
		if err != nil {
			t.Error(err)
		}
		assert.Empty(t, results)
	}
}

func TestUpdateResponseFileImportStatus(t *testing.T) {
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
		rows := []string{"id", "import_status"}
		mock.ExpectQuery(`UPDATE opt_out_file`).
			WithArgs(test.importStatus, id).
			WillReturnRows(sqlmock.NewRows(rows).AddRow(id, test.importStatus))
		err := updateResponseFileImportStatus(db, id, test.importStatus)
		assert.Equal(t, test.err, err)
	}
}

func TestIntegrationToken(t *testing.T) {
	if testing.Short() {
		t.Skip("Skipping integration test.")
	}
	password, err := token(context.TODO(), "postgres-host", 5432, "local-dpc_consent-role")
	assert.Nil(t, err)
	assert.Contains(t, password, "postgres-host:5432?Action=connect&DBUser=local-dpc_consent-role")
}

func TestInsertConsentRecords_ParameterizedArgs(t *testing.T) {
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	fileId := "test_id"
	records := []*OptOutRecord{
		{ID: "rec-1", MBI: "1SJ0A00AA00", PolicyCode: "OPTOUT"},
	}

	rows := []string{"id", "mbi", "effective_date", "policy_code", "opt_out_file_id"}
	// Verify the query receives args and does NOT have raw values in the SQL string
	mock.ExpectQuery("INSERT INTO consent").
		WithArgs("rec-1", "1SJ0A00AA00", "OPTOUT", fileId).
		WillReturnRows(sqlmock.NewRows(rows).
			AddRow("rec-1", "1SJ0A00AA00", time.Date(2019, 7, 1, 0, 0, 0, 0, time.UTC), "OPTOUT", fileId))

	rows2 := []string{"id", "import_status"}
	mock.ExpectQuery("UPDATE opt_out_file").
		WithArgs(ImportComplete, fileId).
		WillReturnRows(sqlmock.NewRows(rows2).AddRow(fileId, ImportComplete))

	results, err := insertConsentRecords(db, fileId, records)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(results))
	assert.Equal(t, Accepted, results[0].Status)
}

func TestInsertConsentRecords_SQLInjectionAttempt(t *testing.T) {
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	fileId := "test_id"
	// Malicious input that would break a string-interpolated query
	records := []*OptOutRecord{
		{ID: "rec-1", MBI: "'); DROP TABLE consent;--", PolicyCode: "OPTOUT"},
	}

	rows := []string{"id", "mbi", "effective_date", "policy_code", "opt_out_file_id"}
	// With parameterized queries the malicious string is treated as a plain value
	mock.ExpectQuery("INSERT INTO consent").
		WithArgs("rec-1", "'); DROP TABLE consent;--", "OPTOUT", fileId).
		WillReturnRows(sqlmock.NewRows(rows).
			AddRow("rec-1", "'); DROP TABLE consent;--", time.Now(), "OPTOUT", fileId))

	rows2 := []string{"id", "import_status"}
	mock.ExpectQuery("UPDATE opt_out_file").
		WithArgs(ImportComplete, fileId).
		WillReturnRows(sqlmock.NewRows(rows2).AddRow(fileId, ImportComplete))

	results, err := insertConsentRecords(db, fileId, records)
	assert.NoError(t, err)
	assert.Equal(t, 1, len(results))
	assert.Equal(t, Accepted, results[0].Status)
}

func TestInsertConsentRecords_MultipleRecordsParamOffsets(t *testing.T) {
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("Unexpected error when opening a mock database %s", err)
	}
	defer db.Close()

	fileId := "test_id"
	records := []*OptOutRecord{
		{ID: "rec-1", MBI: "1SJ0A00AA00", PolicyCode: "OPTOUT"},
		{ID: "rec-2", MBI: "2SJ0A00AA00", PolicyCode: "OPTIN"},
		{ID: "rec-3", MBI: "3SJ0A00AA00", PolicyCode: "OPTOUT"},
	}

	rows := []string{"id", "mbi", "effective_date", "policy_code", "opt_out_file_id"}
	// All 3 records' args passed in order: rec1 args, rec2 args, rec3 args
	mock.ExpectQuery("INSERT INTO consent").
		WithArgs(
			"rec-1", "1SJ0A00AA00", "OPTOUT", fileId,
			"rec-2", "2SJ0A00AA00", "OPTIN", fileId,
			"rec-3", "3SJ0A00AA00", "OPTOUT", fileId,
		).
		WillReturnRows(sqlmock.NewRows(rows).
			AddRow("rec-1", "1SJ0A00AA00", time.Now(), "OPTOUT", fileId).
			AddRow("rec-2", "2SJ0A00AA00", time.Now(), "OPTIN", fileId).
			AddRow("rec-3", "3SJ0A00AA00", time.Now(), "OPTOUT", fileId))

	rows2 := []string{"id", "import_status"}
	mock.ExpectQuery("UPDATE opt_out_file").
		WithArgs(ImportComplete, fileId).
		WillReturnRows(sqlmock.NewRows(rows2).AddRow(fileId, ImportComplete))

	results, err := insertConsentRecords(db, fileId, records)
	assert.NoError(t, err)
	assert.Equal(t, 3, len(results))
	for _, result := range results {
		assert.Equal(t, Accepted, result.Status)
	}
	for _, record := range records {
		assert.Equal(t, Accepted, record.Status)
	}
}
