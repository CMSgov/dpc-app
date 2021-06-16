package repository

import (
	"context"
	"database/sql"
	"time"

	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/pkg/errors"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/google/uuid"
	"github.com/huandu/go-sqlbuilder"
)

// JobRepo is an interface for test mocking purposes
type JobRepo interface {
	GetFileInfo(ctx context.Context, orgID string, fileName string) (*v1.FileInfo, error)
	Insert(ctx context.Context, orgID string, batches []v1.BatchRequest) (*string, error)
	FindBatchesByJobID(id string, orgID string) ([]v1.JobQueueBatch, error)
	FindBatchFilesByBatchID(id string) ([]v1.JobQueueBatchFile, error)
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

// Insert function that saves a slice of JobQueueBatch's into the database and returns an error if there is one
func (jr *JobRepositoryV1) Insert(ctx context.Context, orgID string, batches []v1.BatchRequest) (*string, error) {

	// insert the batches within a single transaction
	tx, err := jr.db.Begin()
	if err != nil {
		return nil, err
	}
	jobID := uuid.New().String()
	for _, b := range batches {
		s, _ := b.Since.Value()
		ib := sqlFlavor.NewInsertBuilder()
		ib.InsertInto("job_queue_batch")
		ib.Cols("batch_id", "job_id", "organization_id", "organization_npi", "provider_npi", "patients", "resource_types", "since",
			"priority", "transaction_time", "status", "submit_time", "request_url", "requesting_ip", "is_bulk")
		batchID := uuid.New().String()
		ib.Values(batchID, jobID, orgID, b.OrganizationNPI, b.ProviderNPI, b.PatientMBIs, b.ResourceTypes, s,
			b.Priority, b.TransactionTime, 0, time.Now(), b.RequestURL, b.RequestingIP, b.IsBulk)
		q, args := ib.Build()
		_, err = tx.ExecContext(ctx, q, args...)
		if err != nil {
			err2 := tx.Rollback()
			if err2 != nil {
				return nil, err2
			}
			return nil, err
		}
	}
	err = tx.Commit()
	if err != nil {
		return nil, err
	}

	return &jobID, nil
}

// FindBatchesByJobID function that returns the batches by job and org id
func (jr *JobRepositoryV1) FindBatchesByJobID(id string, orgID string) ([]v1.JobQueueBatch, error) {
	sb := sqlFlavor.NewSelectBuilder()
	q, args := sb.Select("batch_id", "patients", "transaction_time", "status", "submit_time", "request_url", "patient_index", "complete_time").
		From("job_queue_batch").
		Where(sb.Equal("job_id", id), sb.Equal("organization_id", orgID)).
		Build()
	r, err := jr.db.Query(q, args...)
	if err != nil {
		return nil, err
	}

	batches := make([]v1.JobQueueBatch, 0)
	for r.Next() {
		batch := new(v1.JobQueueBatch)
		batchStruct := sqlbuilder.NewStruct(batch).For(sqlFlavor)
		if err := r.Scan(batchStruct.Addr(&batch)...); err != nil {
			return nil, err
		}
		batches = append(batches, *batch)
	}
	return batches, nil
}

// FindBatchFilesByBatchID function that returns the batch files by batch id
func (jr *JobRepositoryV1) FindBatchFilesByBatchID(id string) ([]v1.JobQueueBatchFile, error) {
	sb := sqlFlavor.NewSelectBuilder()
	q, args := sb.Select("resource_type", "batch_id", "sequence", "file_name", "count", "checksum", "file_length").
		From("job_queue_batch_file").
		Where(sb.Equal("batch_id", id)).
		Build()
	r, err := jr.db.Query(q, args...)
	if err != nil {
		return nil, err
	}

	files := make([]v1.JobQueueBatchFile, 0)
	for r.Next() {
		file := new(v1.JobQueueBatchFile)
		fileStruct := sqlbuilder.NewStruct(file).For(sqlFlavor)
		if err := r.Scan(fileStruct.Addr(&file)...); err != nil {
			return nil, err
		}
		files = append(files, *file)
	}
	return files, nil
}

// GetFileInfo function checks if the file name along with the orgId is valid
func (jr *JobRepositoryV1) GetFileInfo(ctx context.Context, orgID string, fileName string) (*v1.FileInfo, error) {
	log := logger.WithContext(ctx)

	sb := sqlFlavor.NewSelectBuilder()
	sb.Select("f.job_id, b.start_time, f.file_length, f.checksum")
	sb.From("job_queue_batch_file f")
	sb.JoinWithOption(sqlbuilder.LeftJoin, "job_queue_batch b", "b.job_id = f.job_id")
	sb.Where(sb.Equal("f.file_name", fileName), sb.Equal("b.organization_id", orgID))
	q, args := sb.Build()

	var jobID string
	var startTime *time.Time
	var fileLength int
	var checksum []byte
	if err := jr.db.QueryRowContext(ctx, q, args...).Scan(&jobID, &startTime, &fileLength, &checksum); err != nil {
		return nil, err
	}

	if startTime == nil {
		return nil, errors.New("job batch for file doesn't have a valid start time")
	}

	sb = sqlFlavor.NewSelectBuilder()
	sb.Select("status")
	sb.From("job_queue_batch")
	sb.Where(sb.Equal("job_id", jobID))
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

	for rows.Next() {
		var status int
		if err := rows.Scan(&status); err != nil {
			log.Warn("Failed to get status", zap.Error(err))
		}
		if status != 2 {
			return nil, errors.New("Not all job batches are completed")
		}
	}

	return &v1.FileInfo{FileName: fileName, FileLength: fileLength, FileCheckSum: checksum}, nil
}
