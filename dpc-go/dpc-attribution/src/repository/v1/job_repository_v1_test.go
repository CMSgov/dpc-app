package v1

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/pkg/errors"
	"log"
	"testing"
	"time"

	v1 "github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/bxcodec/faker/v3"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

func newMock() (*sql.DB, sqlmock.Sqlmock) {
	db, mock, err := sqlmock.New()
	if err != nil {
		log.Fatalf("an error '%s' was not expected when opening a stub database connection", err)
	}
	return db, mock
}

type JobRepositoryV1TestSuite struct {
	suite.Suite
	fakeBatch   v1.BatchRequest
	fakeNPIs    v1.ProviderOrg
	fakeDetails v1.BatchRequest
}

func (suite *JobRepositoryV1TestSuite) SetupTest() {
	jqb := v1.BatchRequest{}
	err := faker.FakeData(&jqb)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.fakeBatch = jqb

	npis := v1.ProviderOrg{}
	err = faker.FakeData(&npis)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.fakeNPIs = npis

	deets := v1.BatchRequest{}
	err = faker.FakeData(&deets)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.fakeDetails = deets
}

func TestJobRepositoryV1TestSuite(t *testing.T) {
	suite.Run(t, new(JobRepositoryV1TestSuite))
}

func (suite *JobRepositoryV1TestSuite) TestInsertErrorInRepo() {
	db, mock := newMock()
	defer db.Close()
	repo := NewJobRepo(db)
	ctx := context.Background()
	batches := []v1.BatchRequest{suite.fakeBatch}

	mock.ExpectBegin()
	mock.ExpectRollback()
	job, err := repo.Insert(ctx, "", batches)
	if err2 := mock.ExpectationsWereMet(); err2 != nil {
		suite.T().Errorf("there were unfulfilled expectations: %s", err2)
	}
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), job)
}

func (suite *JobRepositoryV1TestSuite) TestInsert() {
	db, mock := newMock()
	defer db.Close()
	repo := NewJobRepo(db)
	ctx := context.Background()
	batches := []v1.BatchRequest{suite.fakeBatch}

	expectedInsertQuery := `INSERT INTO job_queue_batch \(batch_id, job_id, organization_id, organization_npi, provider_npi, patients, resource_types, since, priority, transaction_time, status, submit_time,  request_url, requesting_ip, is_bulk\) VALUES \(\$1, \$2, \$3, \$4, \$5, \$6, \$7, \$8, \$9, \$10, \$11, \$12, \$13, \$14, \$15\)`

	mock.ExpectBegin()
	mock.ExpectExec(expectedInsertQuery).WithArgs(
		sqlmock.AnyArg(),
		sqlmock.AnyArg(),
		"12345",
		suite.fakeBatch.OrganizationNPI,
		suite.fakeBatch.ProviderNPI,
		suite.fakeBatch.PatientMBIs,
		suite.fakeBatch.ResourceTypes,
		suite.fakeBatch.Since,
		suite.fakeBatch.Priority,
		suite.fakeBatch.TransactionTime,
		0,
		sqlmock.AnyArg(),
		suite.fakeBatch.RequestURL,
		suite.fakeBatch.RequestingIP,
		suite.fakeBatch.IsBulk,
	).WillReturnResult(sqlmock.NewResult(1, 1))
	mock.ExpectCommit()
	job, err := repo.Insert(ctx, "12345", batches)
	if err := mock.ExpectationsWereMet(); err != nil {
		suite.T().Errorf("there were unfulfilled expectations: %s", err)
	}
	assert.NoError(suite.T(), err)
	assert.NotEmpty(suite.T(), job)
}

func (suite *JobRepositoryV1TestSuite) TestIsFileValid() {
	db, mock := newMock()
	repo := NewJobRepo(db)

	expectedQuery := `SELECT f.job_id, b.start_time, f.file_length, f.checksum FROM job_queue_batch_file f LEFT JOIN job_queue_batch b ON b.job_id = f.job_id WHERE f.file_name = \$1 AND b.organization_id = \$2`
	rows := sqlmock.NewRows([]string{"job_id", "start_time", "file_length", "checksum"}).
		AddRow("54321", time.Now(), 1, make([]byte, 5))
	mock.ExpectQuery(expectedQuery).WithArgs("fileName", "12345").WillReturnRows(rows)

	expectedStatusQuery := `SELECT status FROM job_queue_batch WHERE job_id = \$1`
	rows = sqlmock.NewRows([]string{"status"}).
		AddRow(2)
	mock.ExpectQuery(expectedStatusQuery).WithArgs("54321").WillReturnRows(rows)

	fi, err := repo.GetFileInfo(context.Background(), "12345", "fileName")

	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), "fileName", fi.FileName)
}

func (suite *JobRepositoryV1TestSuite) TestIsFileValidIncompleteBatches() {
	db, mock := newMock()
	repo := NewJobRepo(db)

	expectedQuery := `SELECT f.job_id, b.start_time, f.file_length, f.checksum FROM job_queue_batch_file f LEFT JOIN job_queue_batch b ON b.job_id = f.job_id WHERE f.file_name = \$1 AND b.organization_id = \$2`
	rows := sqlmock.NewRows([]string{"job_id", "start_time", "file_length", "checksum"}).
		AddRow("54321", time.Now(), 1, make([]byte, 5))
	mock.ExpectQuery(expectedQuery).WithArgs("fileName", "12345").WillReturnRows(rows)

	expectedStatusQuery := `SELECT status FROM job_queue_batch WHERE job_id = \$1`
	rows = sqlmock.NewRows([]string{"status"}).
		AddRow(2).AddRow(1)
	mock.ExpectQuery(expectedStatusQuery).WithArgs("54321").WillReturnRows(rows)

	_, err := repo.GetFileInfo(context.Background(), "12345", "fileName")

	assert.Error(suite.T(), err, "Not all job batches are completed")
}

func (suite *JobRepositoryV1TestSuite) TestFindBatchesByJobIDSQL() {
	db, mock := newMock()
	repo := NewJobRepo(db)
	submitTime := time.Now()
	completeTime := time.Now()

	expectedQuery := `SELECT batch_id, patients, transaction_time, status, submit_time, request_url, patient_index, complete_time FROM job_queue_batch WHERE job_id = \$1 AND organization_id = \$2`
	rows := sqlmock.NewRows([]string{"batch_id", "patients", "transaction_time", "status", "submit_time", "request_url", "patient_index", "complete_time"}).
		AddRow(1, 1, time.Now(), 1, submitTime, "url", 0, completeTime)
	mock.ExpectQuery(expectedQuery).WithArgs("12345", "54321").WillReturnRows(rows)

	batches, err := repo.FindBatchesByJobID("12345", "54321")

	assert.NoError(suite.T(), err)
	assert.Len(suite.T(), batches, 1)
}

func (suite *JobRepositoryV1TestSuite) TestFindBatchesByJobIDSQLErrorHandling() {
	db, mock := newMock()
	repo := NewJobRepo(db)

	expectedQuery := `SELECT batch_id, patients, transaction_time, status, submit_time, request_url, patient_index, complete_time FROM job_queue_batch WHERE job_id = \$1 AND organization_id = \$2`
	mock.ExpectQuery(expectedQuery).WithArgs("12345", "54321").WillReturnError(errors.New("error"))

	batches, err := repo.FindBatchesByJobID("12345", "54321")

	assert.Errorf(suite.T(), err, "error")
	assert.Nil(suite.T(), batches)
}

func (suite *JobRepositoryV1TestSuite) TestFindBatchFilesByBatchIDSQL() {
	db, mock := newMock()
	repo := NewJobRepo(db)

	expectedQuery := `SELECT resource_type, batch_id, sequence, file_name, count, checksum, file_length FROM job_queue_batch_file WHERE batch_id = \$1`
	rows := sqlmock.NewRows([]string{"resource_type", "batch_id", "sequence", "file_name", "count", "checksum", "file_length"}).
		AddRow(77, 1, 0, "testFile", 1, []byte{}, 1234)
	mock.ExpectQuery(expectedQuery).WithArgs("12345").WillReturnRows(rows)

	files, err := repo.FindBatchFilesByBatchID("12345")

	assert.NoError(suite.T(), err)
	assert.Len(suite.T(), files, 1)
}

func (suite *JobRepositoryV1TestSuite) TestFindBatchFilesByBatchIDErrorHandling() {
	db, mock := newMock()
	repo := NewJobRepo(db)

	expectedQuery := `SELECT resource_type, batch_id, sequence, file_name, count, checksum, file_length FROM job_queue_batch_file WHERE batch_id = \$1`
	mock.ExpectQuery(expectedQuery).WithArgs("12345").WillReturnError(errors.New("error"))

	files, err := repo.FindBatchFilesByBatchID("12345")

	assert.Errorf(suite.T(), err, "error")
	assert.Nil(suite.T(), files)
}
