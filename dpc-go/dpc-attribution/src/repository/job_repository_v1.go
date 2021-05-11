package repository

import (
	"context"
	"database/sql"
	"strings"
	"time"

	"github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/google/uuid"
	"github.com/huandu/go-sqlbuilder"
)

// JobRepo is an interface for test mocking purposes
type JobRepo interface {
	NewJobQueueBatch(orgID string, g v1.GroupNPIs, patientMBIs []string, details BatchDetails) *v1.JobQueueBatch
	Insert(ctx context.Context, batches []v1.JobQueueBatch) ([]v1.Job, error)
	GetGroupNPIs(ctx context.Context, groupID string) (*v1.GroupNPIs, error)
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

type BatchDetails struct {
	Priority     int
	Tt           time.Time
	Since        time.Time
	Types        string
	RequestingIP string
}

// NewJobQueueBatch function that creates a new JobQueueBatch
func (jr *JobRepositoryV1) NewJobQueueBatch(orgID string, g v1.GroupNPIs, patientMBIs []string, details BatchDetails) *v1.JobQueueBatch {
	return &v1.JobQueueBatch{
		JobID:           uuid.New(),
		OrganizationID:  orgID,
		OrganizationNPI: g.OrgNPI,
		ProviderNPI:     g.ProviderNPI,
		PatientMBIs:     strings.Join(patientMBIs, ","),
		ResourceTypes:   details.Types,
		Since:           details.Since,
		Priority:        details.Priority,
		Status:          0,
		TransactionTime: details.Tt,
		RequestingIP:    details.RequestingIP,
		IsBulk:          true,
		SubmitTime:      time.Now(),
	}
}

// Insert function that saves a slice of JobQueueBatch's into the database and returns an error if there is one
func (jr *JobRepositoryV1) Insert(ctx context.Context, batches []v1.JobQueueBatch) ([]v1.Job, error) {
	ib := sqlFlavor.NewInsertBuilder()
	ib.InsertInto("job_queue_ batch")
	ib.Cols("job_id", "organization_id", "organization_npi", "provider_npi", "patients", "resource_types", "since",
		"priority", "transaction_time", "status", "submit_time", "requesting_ip", "is_bulk")
	var results []v1.Job
	job := new(v1.Job)
	for _, b := range batches {
		ib.Values(b.JobID, b.OrganizationID, b.OrganizationNPI, b.ProviderNPI, b.PatientMBIs, b.ResourceTypes, b.Since,
			b.Priority, b.TransactionTime, b.Status, b.SubmitTime, b.RequestingIP, b.IsBulk)
		ib.SQL("returning job_id")

		q, args := ib.Build()

		jobStruct := sqlbuilder.NewStruct(*job).For(sqlFlavor)

		// insert the batches within a single transaction
		tx, err := jr.db.Begin()
		if err != nil {
			return nil, err
		}
		if err = tx.QueryRowContext(ctx, q, args...).Scan(jobStruct.Addr(&job)...); err != nil {
			err = tx.Rollback()
			if err != nil {
				return nil, err
			}
			return nil, err
		}
		err = tx.Commit()
		if err != nil {
			return nil, err
		}
		results = append(results, *job)
	}
	return results, nil
}

// GetGroupNPIs function returns an organization NPI and a provider NPI for a given group ID
func (jr *JobRepositoryV1) GetGroupNPIs(ctx context.Context, groupID string) (*v1.GroupNPIs, error) {
	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("o.id_value, p.provider_id")
	sb.From("rosters r")
	sb.Join("organizations o", "r.organization_id = o.id")
	sb.Join("providers p", "r.provider_id = p.id")
	sb.Where(sb.Equal("r.id", groupID))
	q, args := sb.Build()

	row := new(v1.GroupNPIs)
	rowStruct := sqlbuilder.NewStruct(new(v1.GroupNPIs)).For(sqlFlavor)
	if err := jr.db.QueryRowContext(ctx, q, args...).Scan(rowStruct.Addr(&row)...); err != nil {
		return nil, err
	}

	return row, nil
}
