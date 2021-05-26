package service

import (
	"bytes"
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/middleware"
	"net/http"
	"time"

	"github.com/CMSgov/dpc/attribution/conf"
	v1 "github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/CMSgov/dpc/attribution/service"
	"github.com/CMSgov/dpc/attribution/util"
	"github.com/darahayes/go-boom"
	"github.com/pkg/errors"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
)

// JobServiceV1 is a struct that defines what the service has
type JobServiceV1 struct {
	pr repository.PatientRepo
	jr repository.JobRepo
}

// ExportRequest is a struct to hold all of the export request details
type ExportRequest struct {
	orgID             string
	groupID           string
	groupNPIs         *v1.GroupNPIs
	requestURL        string
	requestingIP      string
	since             sql.NullTime
	tt                time.Time
	types             string
	patientMBIBatches [][]string
	totalPatients     int
}

// NewJobService function that creates and returns a JobService
func NewJobService(pr repository.PatientRepo, jr repository.JobRepo) service.JobService {
	return &JobServiceV1{
		pr,
		jr,
	}
}

// Export function that starts an export job for a given Group ID using v1 db
func (js *JobServiceV1) Export(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	var batches []v1.JobQueueBatch
	exportRequest, err := js.buildExportRequest(r.Context(), w)
	if err != nil {
		log.Error("Failed to create job", zap.Error(err))
		boom.BadData(w, err)
		return
	}
	for _, patients := range exportRequest.patientMBIBatches {
		// Set the priority of a job patients
		// Single patients will have first priority to support patient everything
		priority := 5000
		if len(patients) == 1 {
			priority = 1000
		}
		details := repository.BatchDetails{
			Priority:     priority,
			Tt:           exportRequest.tt,
			Since:        exportRequest.since,
			Types:        exportRequest.types,
			RequestURL:   exportRequest.requestURL,
			RequestingIP: exportRequest.requestingIP,
		}
		jobQueueBatch := js.jr.NewJobQueueBatch(exportRequest.orgID, exportRequest.groupNPIs, patients, details)
		batches = append(batches, *jobQueueBatch)
	}
	job, err := js.jr.Insert(r.Context(), batches)
	if err != nil {
		log.Error("Failed to create job", zap.Error(err))
		boom.BadData(w, err)
		return
	}
	jobBytes := new(bytes.Buffer)
	if err := json.NewEncoder(jobBytes).Encode(job); err != nil {
		log.Error("Failed to convert orm model to bytes for job", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}
	if _, err := w.Write(jobBytes.Bytes()); err != nil {
		log.Error("Failed to write job ID to response", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}
	log.Info(fmt.Sprintf(
		"dpcMetric=jobCreated,jobId=%s,orgId=%s,groupId=%s,totalPatients=%x,resourcesRequested=%s",
		job.ID,
		exportRequest.orgID,
		exportRequest.groupID,
		exportRequest.totalPatients,
		exportRequest.types),
	)
}

func (js *JobServiceV1) buildExportRequest(ctx context.Context, w http.ResponseWriter) (*ExportRequest, error) {
	log := logger.WithContext(ctx)
	exportRequest := new(ExportRequest)
	exportRequest.groupID = util.FetchValueFromContext(ctx, w, middleware.ContextKeyGroup)
	exportRequest.orgID = util.FetchValueFromContext(ctx, w, middleware.ContextKeyOrganization)
	exportRequest.requestingIP = util.FetchValueFromContext(ctx, w, middleware.ContextKeyRequestingIP)
	exportRequest.requestURL = util.FetchValueFromContext(ctx, w, middleware.ContextKeyRequestURL)
	patientMBIs, err := js.pr.FindMBIsByGroupID(exportRequest.groupID)
	if err != nil || len(patientMBIs) == 0 {
		if err == nil {
			err = errors.New("Failed to fetch patients for group")
		}
		log.Error("Failed to fetch patients for group", zap.Error(err))
		boom.BadRequest(w, "The group must contain active patients")
		return nil, err
	}
	exportRequest.totalPatients = len(patientMBIs)
	exportRequest.patientMBIBatches = batchPatientMBIs(patientMBIs, conf.GetAsInt("queue.batchSize", 100))
	// TODO: Handle since param
	exportRequest.since = sql.NullTime{}
	// TODO: Handle types param
	exportRequest.types = "Patient,Coverage,ExplanationOfBenefit"
	// TODO: Get TransactionTime from BFD (requires client)
	exportRequest.tt = time.Time{}

	groupNPIs, err := js.pr.GetGroupNPIs(ctx, exportRequest.groupID)
	if err != nil || groupNPIs.OrgNPI == "" || groupNPIs.ProviderNPI == "" {
		log.Error("Failed to retrieve NPIs for Group", zap.Error(err))
		boom.BadData(w, err)
		return nil, err
	}
	exportRequest.groupNPIs = groupNPIs
	return exportRequest, nil
}

func batchPatientMBIs(patientMBIs []string, batchSize int) [][]string {
	var batches [][]string
	for i := 0; i < len(patientMBIs); i += batchSize {
		end := i + batchSize
		// necessary check to avoid slicing beyond slice capacity
		if end > len(patientMBIs) {
			end = len(patientMBIs)
		}
		batches = append(batches, patientMBIs[i:end])
	}
	return batches
}
