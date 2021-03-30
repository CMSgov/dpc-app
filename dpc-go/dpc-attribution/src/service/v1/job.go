package service

import (
	"context"
	"fmt"
	"strings"

	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/logger"
	v1 "github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/CMSgov/dpc/attribution/repository"
)

// JobServiceV1 is a struct that defines what the service has
type JobServiceV1 struct {
	pr repository.PatientRepo
	jr repository.JobRepo
}

// NewJobServiceV1 function that creates a job service and returns its reference
func NewJobServiceV1(pr repository.PatientRepo, jr repository.JobRepo) *JobServiceV1 {
	return &JobServiceV1{
		pr,
		jr,
	}
}

// Export function that starts an export job for a given Group ID using v1 db
func (js *JobServiceV1) Export(ctx context.Context, groupID string, orgID string) (string, error) {
	log := logger.WithContext(ctx)
	patientMBIs, err := js.pr.FindMBIsByGroupID(groupID)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to fetch patients for group"), zap.Error(err))
		return "", err
	}
	// TODO: handle Type query param
	// TODO: handle _since query param
	// TODO: set requesting IP address
	// TODO: return a job UUID from the create job func (start job)
	batch := js.createNewJobBatch(orgID, patientMBIs)
	jobID, err := js.jr.Insert(ctx, batch)
	if err != nil {
		return "", err
	}
	return jobID, nil
}

func (js *JobServiceV1) createNewJobBatch(orgID string, patientMBIs []string) v1.JobQueueBatch {
	return v1.JobQueueBatch{
		nil,
		orgID,
		nil,
		strings.Join(patientMBIs, ","),
		nil,
		nil,
		nil,
		0,
		nil,
		nil,
		true,
	}
}
