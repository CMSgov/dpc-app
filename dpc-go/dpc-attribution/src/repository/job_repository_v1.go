package repository

import (
	"context"
	"database/sql"

	"github.com/CMSgov/dpc/attribution/model/v1"
)

// JobRepo is an interface for test mocking purposes
type JobRepo interface {
	Insert(ctx context.Context, batch v1.JobQueueBatch) (string, error)
}

// JobRepositoryV1 is a struct that defines what the repository has
type JobRepositoryV1 struct {
	db *sql.DB
}

// NewJobRepo function that creates a jobRepository and returns its reference
func NewJobRepo(db *sql.DB) *JobRepositoryV1 {
	return &JobRepositoryV1{
		db,
	}
}

// Insert function that saves a JobQueueBatch into the database and returns an error if there is one
func (jr *JobRepositoryV1) Insert(ctx context.Context, b v1.JobQueueBatch) (string, error) {
	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto("job_queue_ batch")
	ib.Cols("organization_id", "organization_npi", "provider_npi", "patients", "resource_types", "since",
		"transaction_time", "status", "submit_time", "requesting_ip", "is_bulk")
	ib.Values(b.OrganizationID, b.OrganizationNPI, b.ProviderNPI, b.PatientMBIs, b.ResourceTypes, b.Since,
		b.TransactionTime, b.Status, b.SubmitTime, b.RequestingIP, b.IsBulk)
	ib.SQL("returning job_id")

	q, args := ib.Build()

	var jobID string

	if err := jr.db.QueryRowContext(ctx, q, args...).Scan(&jobID); err != nil {
		return "", err
	}
	return jobID, nil
}
