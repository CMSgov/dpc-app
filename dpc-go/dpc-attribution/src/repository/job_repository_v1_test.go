package repository

import (
	"context"
	"fmt"
	"testing"

	v1 "github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/bxcodec/faker/v3"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type JobRepositoryV1TestSuite struct {
	suite.Suite
	fakeJQB     v1.JobQueueBatch
	fakeNPIs    v1.GroupNPIs
	fakeDetails BatchDetails
}

func (suite *JobRepositoryV1TestSuite) SetupTest() {
	jqb := v1.JobQueueBatch{}
	err := faker.FakeData(&jqb)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.fakeJQB = jqb

}

func TestJobRepositoryV1TestSuite(t *testing.T) {
	suite.Run(t, new(JobRepositoryV1TestSuite))
}

func (suite *JobRepositoryV1TestSuite) TestInsertErrorInRepo() {
	db, mock := newMock()
	defer db.Close()
	repo := NewJobRepo(db)
	ctx := context.Background()
	batches := []v1.JobQueueBatch{suite.fakeJQB}

	expectedInsertQuery := `INSERT INTO job_queue_batch \(job_id, organization_id, organization_npi, provider_npi, patients, resource_types, since, priority, transaction_time, status, submit_time, requesting_ip, is_bulk\) VALUES \(\$1, \$2, \$3, \$4, \$5, \$6, \$7, \$8, \$9, \$10, \$11, \$12, \$13\) returning job_id`

	mock.ExpectBegin()
	mock.ExpectQuery(expectedInsertQuery).WithArgs(suite.fakeJQB).WillReturnError(errors.New("hit an error"))
	mock.ExpectRollback()
	job, err := repo.Insert(ctx, batches)
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), job)
}

func (suite *JobRepositoryV1TestSuite) TestInsert() {
	db, mock := newMock()
	defer db.Close()
	repo := NewJobRepo(db)
	ctx := context.Background()
	batches := []v1.JobQueueBatch{suite.fakeJQB}

	expectedInsertQuery := `INSERT INTO job_queue_batch \(job_id, organization_id, organization_npi, provider_npi, patients, resource_types, since, priority, transaction_time, status, submit_time, requesting_ip, is_bulk\) VALUES \(\$1, \$2, \$3, \$4, \$5, \$6, \$7, \$8, \$9, \$10, \$11, \$12, \$13\) returning job_id`

	rows := sqlmock.NewRows([]string{"job_id"}).AddRow(suite.fakeJQB.JobID)
	mock.ExpectBegin()
	mock.ExpectQuery(expectedInsertQuery).WithArgs(
		suite.fakeJQB.JobID,
		suite.fakeJQB.OrganizationID,
		suite.fakeJQB.OrganizationNPI,
		suite.fakeJQB.ProviderNPI,
		suite.fakeJQB.PatientMBIs,
		suite.fakeJQB.ResourceTypes,
		suite.fakeJQB.Since,
		suite.fakeJQB.Priority,
		suite.fakeJQB.TransactionTime,
		suite.fakeJQB.Status,
		suite.fakeJQB.SubmitTime,
		suite.fakeJQB.RequestingIP,
		suite.fakeJQB.IsBulk,
	).WillReturnRows(rows)
	mock.ExpectCommit()
	job, err := repo.Insert(ctx, batches)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeJQB.JobID, job.ID)
}
