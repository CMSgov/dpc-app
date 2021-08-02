package service

import (
	"database/sql"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/conf"
	"github.com/CMSgov/dpc/attribution/repository"
	"io/ioutil"
	"net/http"
	"strings"
	"time"

	"github.com/CMSgov/dpc/attribution/client"
	"github.com/CMSgov/dpc/attribution/middleware"
	"github.com/google/uuid"

	"github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/CMSgov/dpc/attribution/util"
	"github.com/darahayes/go-boom"
	"github.com/pkg/errors"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/logger"
	v1Repo "github.com/CMSgov/dpc/attribution/repository/v1"
)

// JobService is an interface that defines what a JobService does
type JobService interface {
	Export(w http.ResponseWriter, r *http.Request)
	BatchesAndFiles(w http.ResponseWriter, r *http.Request)
}

// JobServiceV1 is a struct that defines what the service has
type JobServiceV1 struct {
	jr        v1Repo.JobRepo
	or        repository.OrganizationRepo
	bfdClient client.APIClient
}

// NewJobService function that creates and returns a JobService
func NewJobService(jr v1Repo.JobRepo, or repository.OrganizationRepo, bfdClient client.APIClient) *JobServiceV1 {
	return &JobServiceV1{
		jr,
		or,
		bfdClient,
	}
}

// Export function that starts an export job for a given Group ID using v1 db
func (js JobServiceV1) Export(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	orgID := util.FetchValueFromContext(r.Context(), w, middleware.ContextKeyOrganization)
	org, err := js.or.FindByID(r.Context(), orgID)
	if err != nil {
		log.Error("Failed to get org", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	npi, err := org.GetNPI()
	if err != nil {
		log.Error("Failed to get NPI", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	var er v1.ExportRequest
	b, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Error("Failed to read request", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if err := json.Unmarshal(b, &er); err != nil {
		log.Error("Failed to parse request", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	since, err := parseSinceParam(er.Since)
	if err != nil {
		log.Error("Failed to parse since", zap.Error(err))
		boom.BadData(w, err.Error())
		return
	}

	url := r.Header.Get(middleware.RequestURLHeader)
	ip := r.Header.Get(middleware.FwdHeader)
	batches, err := js.buildV1Batches(er, npi, url, ip, since)
	if err != nil {
		boom.Internal(w, err.Error())
		return
	}
	job, err := js.jr.Insert(r.Context(), orgID, batches)
	if err != nil {
		log.Error("Failed to create job", zap.Error(err))
		boom.BadData(w, err.Error())
		return
	}

	log.Info(fmt.Sprintf(
		"dpcMetric=jobCreated,jobId=%s,orgId=%s,groupId=%s,totalPatients=%x,resourcesRequested=%s",
		*job,
		orgID,
		er.GroupID,
		len(er.MBIs),
		strings.ReplaceAll(er.Type, ",", ";")),
	)

	if _, err := w.Write([]byte(*job)); err != nil {
		log.Error("Failed to write job ID to response", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}
}

func (js JobServiceV1) fetchTransactionTime() (*time.Time, error) {
	b, err := js.bfdClient.GetPatient("FAKE_PATIENT", uuid.New().String(), uuid.New().String(), "", time.Now())
	if err != nil {
		return nil, err
	}
	if b.Meta.LastUpdated.Equal(time.Time{}) {
		return nil, errors.New("No transaction time returned from BFD")
	}
	return &b.Resource.Meta.LastUpdated, nil
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

func (js *JobServiceV1) buildV1Batches(er v1.ExportRequest, orgNPI string, url string, ip string, since *sql.NullTime) ([]v1.BatchRequest, error) {
	priority := 5000
	if len(er.MBIs) == 1 {
		priority = 1000
	}

	tt, err := js.fetchTransactionTime()
	if err != nil {
		return nil, errors.Errorf("Failed to fetch Transaction Time from BFD: %s", err.Error())
	}

	patientBatches := batchPatientMBIs(er.MBIs, conf.GetAsInt("queue.batchSize", 100))
	var batches []v1.BatchRequest
	for _, batchedPatients := range patientBatches {
		batch := v1.BatchRequest{
			Since:           since,
			RequestURL:      url,
			RequestingIP:    ip,
			OrganizationNPI: orgNPI,
			ProviderNPI:     er.ProviderNPI,
			PatientMBIs:     strings.Join(batchedPatients, ","),
			IsBulk:          len(batchedPatients) > 1,
			ResourceTypes:   er.Type,
		}
		batch.Priority = priority
		if since.Valid && !tt.After(since.Time) {
			batch.PatientMBIs = ""
		}
		batch.TransactionTime = *tt
		batches = append(batches, batch)
	}
	return batches, nil
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
