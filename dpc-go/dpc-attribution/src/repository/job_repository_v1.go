package repository

import (
	"context"
	"database/sql"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/pkg/errors"
	"go.uber.org/zap"
	"strings"
	"time"

	"github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/google/uuid"
	"github.com/huandu/go-sqlbuilder"
)

// JobRepo is an interface for test mocking purposes
type JobRepo interface {
	NewJobQueueBatch(orgID string, g *v1.GroupNPIs, patientMBIs []string, details BatchDetails) *v1.JobQueueBatch
	Insert(ctx context.Context, batches []v1.JobQueueBatch) (*v1.Job, error)
	IsFileValid(ctx context.Context, orgID string, fileName string) (*v1.FileInfo, error)
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

// BatchDetails is a struct to hold details for a JobQueueBatch
type BatchDetails struct {
	Priority     int
	Tt           time.Time
	Since        sql.NullTime
	Types        string
	RequestURL   string
	RequestingIP string
}

// NewJobQueueBatch function that creates a new JobQueueBatch
func (jr *JobRepositoryV1) NewJobQueueBatch(orgID string, g *v1.GroupNPIs, patientMBIs []string, details BatchDetails) *v1.JobQueueBatch {
	return &v1.JobQueueBatch{
		OrganizationID:  orgID,
		OrganizationNPI: g.OrgNPI,
		ProviderNPI:     g.ProviderNPI,
		PatientMBIs:     strings.Join(patientMBIs, ","),
		ResourceTypes:   details.Types,
		Since:           details.Since,
		Priority:        details.Priority,
		Status:          0,
		TransactionTime: details.Tt,
		RequestURL:      details.RequestURL,
		RequestingIP:    details.RequestingIP,
		IsBulk:          true,
		SubmitTime:      time.Now(),
	}
}

// Insert function that saves a slice of JobQueueBatch's into the database and returns an error if there is one
func (jr *JobRepositoryV1) Insert(ctx context.Context, batches []v1.JobQueueBatch) (*v1.Job, error) {
	var results []*v1.Job
	job := new(v1.Job)
	// insert the batches within a single transaction
	tx, err := jr.db.Begin()
	if err != nil {
		return nil, err
	}
	jobID := uuid.New().String()
	for _, b := range batches {
		ib := sqlFlavor.NewInsertBuilder()
		ib.InsertInto("job_queue_batch")
		ib.Cols("batch_id", "job_id", "organization_id", "organization_npi", "provider_npi", "patients", "resource_types", "since",
			"priority", "transaction_time", "status", "submit_time", "request_url", "requesting_ip", "is_bulk")
		batchID := uuid.New().String()
		ib.Values(batchID, jobID, b.OrganizationID, b.OrganizationNPI, b.ProviderNPI, b.PatientMBIs, b.ResourceTypes, b.Since,
			b.Priority, b.TransactionTime, b.Status, b.SubmitTime, b.RequestURL, b.RequestingIP, b.IsBulk)
		ib.SQL("returning job_id")
		q, args := ib.Build()
		jobStruct := sqlbuilder.NewStruct(job).For(sqlFlavor)
		if err := tx.QueryRowContext(ctx, q, args...).Scan(jobStruct.Addr(&job)...); err != nil {
			err2 := tx.Rollback()
			if err2 != nil {
				return nil, err2
			}
			return nil, err
		}
		results = append(results, job)
	}
	err = tx.Commit()
	if err != nil || len(results) == 0 {
		return nil, err
	}
	return results[0], nil
}

func (jr *JobRepositoryV1) IsFileValid(ctx context.Context, orgID string, fileName string) (*v1.FileInfo, error) {
	log := logger.WithContext(ctx)

	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("f.job_id, b.start_time, f.file_length, f.checksum")
	sb.From("job_queue_batch_file f")
	sb.JoinWithOption(sqlbuilder.LeftJoin, "job_queue_batch b", "b.job_id = f.job_id")
	sb.Where(sb.Equal("f.file_name", fileName), sb.Equal("b.organization_id", orgID))
	q, args := sb.Build()

	var jobId string
	var startTime *time.Time
	var fileLength int
	var checksum []byte
	if err := jr.db.QueryRowContext(ctx, q, args...).Scan(&jobId, &startTime, &fileLength, &checksum); err != nil {
		return nil, err
	}

	if startTime == nil {
		return nil, errors.New("job batch for file doesn't have a valid start time")
	}

	sb = sqlFlavor.NewSelectBuilder()
	sb.Select("status")
	sb.From("job_queue_batch")
	sb.Where(sb.Equal("job_id", jobId))
	q, args = sb.Build()

	rows, err := jr.db.Query(q, args...)
	defer func() {
		err := rows.Close()
		if err != nil {
			log.Warn("Failed to close rows", zap.Error(err))
		}
	}()

	if err != nil {
		return nil, err
	}

	statuses := make([]int, 0)
	for rows.Next() {
		var status int
		if err := rows.Scan(&status); err != nil {
			log.Warn("Failed to get status", zap.Error(err))
		}
		statuses = append(statuses, status)
	}

	for _, v := range statuses {
		if v != 2 {
			return nil, errors.New("Not all job batches are completed")
		}
	}

	return &v1.FileInfo{FileName: fileName, FileLength: fileLength, FileCheckSum: checksum}, nil
}
