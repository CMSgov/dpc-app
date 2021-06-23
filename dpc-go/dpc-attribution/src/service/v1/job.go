package service

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/CMSgov/dpc/attribution/client"
	"github.com/CMSgov/dpc/attribution/middleware"
	"github.com/google/uuid"

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

// ExportInfo is a struct containing details needed to export data
type ExportInfo struct {
	OrgID   string
	GroupID string
	Types   string
	IP      string
	URL     string
	Since   *sql.NullTime
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
	ei := buildExportInfo(w, r)
	if ei == nil {
		return
	}
	groupNPIs, err := js.pr.GetGroupNPIs(r.Context(), ei.GroupID)
	if err != nil || groupNPIs.OrgNPI == "" || groupNPIs.ProviderNPI == "" {
		if err == nil {
			err = errors.New("Failed to retrieve NPIs for Group")
		}
		log.Error("Failed to retrieve NPIs for Group", zap.Error(err))
		boom.BadData(w, err.Error())
		return
	}

	patientMBIs, err := js.pr.FindMBIsByGroupID(ei.GroupID)
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

	tt, err := fetchTransactionTime()
	if err != nil {
		log.Error("Failed to fetch Transaction Time from BFD", zap.Error(err))
		boom.Internal(w, "Failed to start job.")
		return
	}

	batches := buildBatches(ei, groupNPIs, patientMBIs, tt)

	job, err := js.jr.Insert(r.Context(), ei.OrgID, batches)
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
		ei.OrgID,
		ei.GroupID,
		len(patientMBIs),
		strings.ReplaceAll(ei.Types, ",", ";")),
	)
}

func fetchTransactionTime() (time.Time, error) {
	bfd, err := client.NewBfdClient(client.NewConfig("/v1/fhir/"))
	if err != nil {
		return time.Time{}, err
	}
	b, err := bfd.GetPatient("FAKE_PATIENT", uuid.New().String(), uuid.New().String(), "", time.Now())
	if err != nil {
		return time.Time{}, err
	}
	return b.Meta.LastUpdated, nil
}

func buildExportInfo(w http.ResponseWriter, r *http.Request) *ExportInfo {
	log := logger.WithContext(r.Context())
	orgID := util.FetchValueFromContext(r.Context(), w, middleware.ContextKeyOrganization)
	groupID := util.FetchValueFromContext(r.Context(), w, middleware.ContextKeyGroup)
	types := r.URL.Query().Get("_type")
	since, err := parseSinceParam(r.URL.Query().Get("_since"))
	if err != nil {
		err = errors.New("Failed to parse _since param")
		log.Error("Failed to parse _since param", zap.Error(err))
		boom.BadData(w, err.Error())
		return nil
	}
	requestIP := r.Header.Get(middleware.FwdHeader)
	requestURL := r.Header.Get(middleware.RequestURLHeader)
	return &ExportInfo{
		orgID, groupID, types, requestIP, requestURL, since,
	}

}

func parseSinceParam(since string) (*sql.NullTime, error) {
	if since == "" {
		return &sql.NullTime{}, nil
	}
	t, err := time.Parse(middleware.SinceLayout, since)
	if err != nil {
		return nil, err
	}
	return &sql.NullTime{Time: t, Valid: true}, nil
}

func buildBatches(ei *ExportInfo, groupNPIs *v1.GroupNPIs, patientMBIs []string, tt time.Time) []v1.BatchRequest {
	priority := 5000
	if len(patientMBIs) == 1 {
		priority = 1000
	}

	patientBatches := batchPatientMBIs(patientMBIs, conf.GetAsInt("queue.batchSize", 100))
	var batches []v1.BatchRequest
	for _, batchedPatients := range patientBatches {
		batch := v1.BatchRequest{
			Priority:        priority,
			Since:           ei.Since,
			RequestURL:      ei.URL,
			RequestingIP:    ei.IP,
			OrganizationNPI: groupNPIs.OrgNPI,
			ProviderNPI:     groupNPIs.ProviderNPI,
			PatientMBIs:     strings.Join(batchedPatients, ","),
			IsBulk:          len(patientMBIs) > 1,
			ResourceTypes:   ei.Types,
			TransactionTime: tt,
		}
		batches = append(batches, batch)
	}
	return batches
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
			Batch: v1.NewBatchInfo(b),
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
