package service

import (
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/middleware"
	"net/http"
	"strings"
	"time"

	"github.com/CMSgov/dpc/attribution/conf"
	"github.com/CMSgov/dpc/attribution/model/v1"
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

	groupID := util.FetchValueFromContext(r.Context(), w, middleware.ContextKeyGroup)
	orgID := util.FetchValueFromContext(r.Context(), w, middleware.ContextKeyOrganization)
	types := "Patient,Coverage,ExplanationOfBenefit"
	requestIP := r.Header.Get(middleware.FwdHeader)
	requestURL := r.Header.Get(middleware.RequestURLHeader)

	groupNPIs, err := js.pr.GetGroupNPIs(r.Context(), groupID)
	if err != nil || groupNPIs.OrgNPI == "" || groupNPIs.ProviderNPI == "" {
		if err == nil {
			err = errors.New("Failed to retrieve NPIs for Group")
		}
		log.Error("Failed to retrieve NPIs for Group", zap.Error(err))
		boom.BadData(w, err.Error())
		return
	}

	patientMBIs, err := js.pr.FindMBIsByGroupID(groupID)
	if err != nil {
		log.Error("Failed to retrieve patients", zap.Error(err))
		boom.BadData(w, err.Error())
		return
	}

	if len(patientMBIs) == 0 {
		log.Error("No patients to process")
		boom.BadData(w, "No patients to process")
		return
	}

	batches, err := buildBatches(nil, types, requestIP, requestURL, groupNPIs, patientMBIs)
	if err != nil {
		log.Error("Failed to build batches", zap.Error(err))
		boom.BadData(w, err.Error())
	}

	job, err := js.jr.Insert(r.Context(), orgID, batches)
	if err != nil {
		log.Error("Failed to create job", zap.Error(err))
		boom.BadData(w, err.Error())
		return
	}

	if _, err := w.Write([]byte(*job)); err != nil {
		log.Error("Failed to write job ID to response", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}
	log.Info(fmt.Sprintf(
		"dpcMetric=jobCreated,jobId=%s,orgId=%s,groupId=%s,totalPatients=%x,resourcesRequested=%s",
		*job,
		orgID,
		groupID,
		len(patientMBIs),
		types),
	)
}

func buildBatches(since *time.Time, types string, requestIP string, requestURL string, groupNPIs *v1.GroupNPIs, patientMBIs []string) ([]v1.BatchRequest, error) {
	priority := 5000
	if len(patientMBIs) == 1 {
		priority = 1000
	}

	patients := batchPatientMBIs(patientMBIs, conf.GetAsInt("queue.batchSize", 100))
	batches := make([]v1.BatchRequest, len(patients))
	for _, batchedPatients := range patients {
		batch := new(v1.BatchRequest)
		batch.RequestingIP = requestIP
		batch.RequestURL = requestURL
		batch.PatientMBIs = strings.Join(batchedPatients, ",")
		batch.IsBulk = len(patientMBIs) > 1
		batch.ResourceTypes = types
		batch.Since = since
		batch.Priority = priority
		batch.TransactionTime = time.Now() // need a bfd client to do this
		batch.ProviderNPI = groupNPIs.ProviderNPI
		batch.OrganizationNPI = groupNPIs.OrgNPI
	}
	return batches, nil
}

// BatchesAndFiles function returns all the batches and it's files for a job
func (js *JobServiceV1) BatchesAndFiles(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	orgID := util.FetchValueFromContext(r.Context(), w, middleware.ContextKeyOrganization)
	jobID := util.FetchValueFromContext(r.Context(), w, middleware.ContextKeyJobID)

	response := make([]v1.BatchAndFiles, 0)

	batches, err := js.jr.FindBatchesByJobID(jobID, orgID)
	if err != nil {
		log.Error("Failed to find batches by job id", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	for _, b := range batches {
		files, err := js.jr.FindBatchFilesByBatchID(b.BatchID)
		if err != nil {
			log.Error("Failed to retrieve batch files", zap.Error(err))
			boom.Internal(w, err.Error())
			return
		}
		bf := v1.BatchAndFiles{
			Batch: v1.NewBatchInfo(&b),
			Files: files,
		}
		response = append(response, bf)
	}

	b, err := json.Marshal(response)
	if err != nil {
		log.Error("Failed to write json bytes", zap.Error(err))
		boom.Internal(w, err.Error())
	}

	if _, err := w.Write(b); err != nil {
		log.Error("Failed to write organization to response for organization", zap.Error(err))
		boom.Internal(w, err.Error())
	}
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
