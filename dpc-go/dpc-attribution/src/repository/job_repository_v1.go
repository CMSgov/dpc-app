package repository

import (
	"context"
	"database/sql"

	"github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/huandu/go-sqlbuilder"
)

// JobRepo is an interface for test mocking purposes
type JobRepo interface {
	Insert(ctx context.Context, batch v1.JobQueueBatch) (*v1.Job, error)
	GetNPIs(ctx context.Context, groupID string) (*v1.GroupNPIs, error)
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
func (jr *JobRepositoryV1) Insert(ctx context.Context, b v1.JobQueueBatch) (*v1.Job, error) {
	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto("job_queue_ batch")
	ib.Cols("organization_id", "organization_npi", "provider_npi", "patients", "resource_types", "since",
		"transaction_time", "status", "submit_time", "requesting_ip", "is_bulk")
	ib.Values(b.OrganizationID, b.OrganizationNPI, b.ProviderNPI, b.PatientMBIs, b.ResourceTypes, b.Since,
		b.TransactionTime, b.Status, b.SubmitTime, b.RequestingIP, b.IsBulk)
	ib.SQL("returning job_id")

	q, args := ib.Build()

	job := new(v1.Job)
	jobStruct := sqlbuilder.NewStruct(new(v1.Job)).For(sqlFlavor)
	if err := jr.db.QueryRowContext(ctx, q, args...).Scan(jobStruct.Addr(&job)...); err != nil {
		return nil, err
	}

	return job, nil
}

// GetNPIs function returns an organization NPI and a provider NPI for a given group ID
func (gr *JobRepositoryV1) GetNPIs(ctx context.Context, groupID string) (*v1.GroupNPIs, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("o.id_value, p.provider_id")
	sb.From("rosters r")
	sb.Join("organizations o", "r.organization_id = o.id")
	sb.Join("providers p", "r.provider_id = p.id")
	sb.Where(sb.Equal("r.id", groupID))
	q, args := sb.Build()

	row := new(v1.GroupNPIs)
	rowStruct := sqlbuilder.NewStruct(new(v1.GroupNPIs)).For(sqlFlavor)
	if err := gr.db.QueryRowContext(ctx, q, args...).Scan(rowStruct.Addr(&row)...); err != nil {
		return nil, err
	}

	return row, nil
}
