package repository

import (
	"database/sql"

	"github.com/CMSgov/dpc/attribution/model"
)

// JobRepo is an interface for test mocking purposes
type JobRepo interface {
	Insert(batch model.JobQueueBatch) (int, error)
}

// JobRepository is a struct that defines what the repository has
type JobRepository struct {
	db *sql.DB
}

// NewJobRepo function that creates a jobRepository and returns its reference
func NewJobRepo(db *sql.DB) *JobRepository {
	return &JobRepository{
		db,
	}
}

// Insert function that saves a JobQueueBatch into the database and returns an error if there is one
func (jr *JobRepository) Insert(b model.JobQueueBatch) (int, error) {
	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto("job_queue_ batch")
	ib.Cols("organization_id", "provider_id", "patients", "resource_types", "since",
		"transaction_time", "status", "submit_time", "requesting_ip", "is_bulk")
	ib.Values(b.OrganizationId, b.ProviderId, b.PatientMBIs, b.ResourceTypes, b.Since,
		b.TransactionTime, b.Status, b.SubmitTime, b.RequestingIP, b.IsBulk)

	q, args := ib.Build()
	result, err := jr.db.Exec(q, args...)
	if err != nil {
		return 0, err
	}
	c, err := result.RowsAffected()
	return int(c), err
}
