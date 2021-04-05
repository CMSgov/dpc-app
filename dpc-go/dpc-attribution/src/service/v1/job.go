package service

import (
	"context"
	"fmt"
	"net/http"
	"strings"

	"github.com/darahayes/go-boom"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/logger"
	v1 "github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/CMSgov/dpc/attribution/service"
)

type contextKeyString string

// ContextKeyIP is the key in the context to retrieve the requesting IP
const ContextKeyIP contextKeyString = ""

// JobServiceV1 is a struct that defines what the service has
type JobServiceV1 struct {
	pr repository.PatientRepo
	jr repository.JobRepo
}

// NewJobServiceV1 function that creates a job service and returns its reference
func NewJobServiceV1(pr repository.PatientRepo, jr repository.JobRepo) service.JobService {
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
	requestingIP, ok := ctx.Value(ContextKeyIP).(string)
	if !ok {
		log.Error("Failed to extract group id from context")
		boom.BadRequest(w, "Could not get group id")
		return
	}
	// TODO: break patients into batches
	batch := js.createNewJobBatch(orgID, patientMBIs)
	jobID, err := js.jr.Insert(ctx, batch)
	if err != nil {
		return "", err
	}
	return jobID, nil
}

func (js *JobServiceV1) createNewJobBatch(orgID string, patientMBIs []string) v1.JobQueueBatch {
	return v1.JobQueueBatch{
		JobID:           nil,
		OrganizationID:  orgID,
		ProviderID:      nil,
		PatientMBIs:     strings.Join(patientMBIs, ","),
		ResourceTypes:   nil,
		Since:           nil,
		TransactionTime: nil,
		SubmitTime:      nil,
		RequestingIP:    nil,
		IsBulk:          true,
	}
}
