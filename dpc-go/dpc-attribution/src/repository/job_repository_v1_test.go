package repository

import (
	"context"
	"fmt"
	"testing"

	v1 "github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/DATA-DOG/go-sqlmock"
	"github.com/bxcodec/faker/v3"
	"github.com/pkg/errors"
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

	npis := v1.GroupNPIs{}
	err = faker.FakeData(&npis)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.fakeNPIs = npis

	deets := BatchDetails{}
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
	batches := []v1.JobQueueBatch{suite.fakeJQB}

	mock.ExpectBegin()
	mock.ExpectRollback()
	job, err := repo.Insert(ctx, batches)
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
	batches := []v1.JobQueueBatch{suite.fakeJQB}

	expectedInsertQuery := `INSERT INTO job_queue_batch \(job_id, organization_id, organization_npi, provider_npi, patients, resource_types, since, priority, transaction_time, status, submit_time,  request_url, requesting_ip, is_bulk\) VALUES \(\$1, \$2, \$3, \$4, \$5, \$6, \$7, \$8, \$9, \$10, \$11, \$12, \$13, \$14\) returning job_id`

	rows := sqlmock.NewRows([]string{"job_id"}).
		AddRow(suite.fakeJQB.JobID).
		AddRow(suite.fakeJQB.JobID)
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
		suite.fakeJQB.RequestURL,
		suite.fakeJQB.RequestingIP,
		suite.fakeJQB.IsBulk,
	).WillReturnRows(rows)
	mock.ExpectCommit()
	job, err := repo.Insert(ctx, batches)
	if err := mock.ExpectationsWereMet(); err != nil {
		suite.T().Errorf("there were unfulfilled expectations: %s", err)
	}
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), suite.fakeJQB.JobID, job.ID)
}

func (suite *JobRepositoryV1TestSuite) TestGetGroupNPIsErrorInRepo() {
	db, mock := newMock()
	defer db.Close()
	repo := NewJobRepo(db)
	ctx := context.Background()
	groupID := faker.Word()

	expectedInsertQuery := `SELECT o.id_value, p.provider_id FROM rosters r JOIN organizations o ON r.organization_id = o.id JOIN providers p ON r.provider_id = p.id WHERE r.id = \$1`

	mock.ExpectQuery(expectedInsertQuery).WithArgs().WillReturnError(errors.New("Not enough arguments"))
	groupNPIs, err := repo.GetGroupNPIs(ctx, groupID)
	if err2 := mock.ExpectationsWereMet(); err2 != nil {
		suite.T().Errorf("there were unfulfilled expectations: %s", err2)
	}
	assert.Error(suite.T(), err)
	assert.Empty(suite.T(), groupNPIs)
}

func (suite *JobRepositoryV1TestSuite) TestGetGroupNPIs() {
	db, mock := newMock()
	defer db.Close()
	repo := NewJobRepo(db)
	ctx := context.Background()
	groupID := faker.WORD

	expectedInsertQuery := `SELECT o.id_value, p.provider_id FROM rosters r JOIN organizations o ON r.organization_id = o.id JOIN providers p ON r.provider_id = p.id WHERE r.id = \$1`

	rows := sqlmock.NewRows([]string{"organization_npi", "provider_npi"}).AddRow(suite.fakeNPIs.OrgNPI, suite.fakeNPIs.ProviderNPI)
	mock.ExpectQuery(expectedInsertQuery).WithArgs(groupID).WillReturnRows(rows)
	groupNPIs, err := repo.GetGroupNPIs(ctx, groupID)
	assert.NoError(suite.T(), err)
	assert.Equal(suite.T(), &suite.fakeNPIs, groupNPIs)
}

func (suite *JobRepositoryV1TestSuite) TestNewJobQueueBatch() {
	db, _ := newMock()
	defer db.Close()
	repo := NewJobRepo(db)
	orgID := faker.UUIDHyphenated()
	patientMBIs := []string{faker.UUIDHyphenated(), faker.UUIDHyphenated(), faker.UUIDHyphenated()}
	result := repo.NewJobQueueBatch(orgID, &suite.fakeNPIs, patientMBIs, suite.fakeDetails)
	assert.True(suite.T(), isJobQueueBatch(result))
	assert.Equal(suite.T(), orgID, result.OrganizationID)
	assert.Equal(suite.T(), suite.fakeNPIs.OrgNPI, result.OrganizationNPI)
	assert.Equal(suite.T(), suite.fakeNPIs.ProviderNPI, result.ProviderNPI)
	assert.Equal(suite.T(), suite.fakeDetails.Priority, result.Priority)
	assert.Equal(suite.T(), suite.fakeDetails.RequestingIP, result.RequestingIP)
	assert.Equal(suite.T(), suite.fakeDetails.Types, result.ResourceTypes)
}

func isJobQueueBatch(t interface{}) bool {
	switch t.(type) {
	case *v1.JobQueueBatch:
		return true
	default:
		return false
	}
}
