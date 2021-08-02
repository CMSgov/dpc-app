package service

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/conf"
	"github.com/CMSgov/dpc/attribution/model"
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
	"github.com/CMSgov/dpc/attribution/repository"
	v1Repo "github.com/CMSgov/dpc/attribution/repository/v1"
)

// JobService is an interface that defines what a JobService does
type JobService interface {
	Export(w http.ResponseWriter, r *http.Request)
	BatchesAndFiles(w http.ResponseWriter, r *http.Request)
}

type exportResponse struct {
	JobIDs []string `json:"jobIDs"`
}

type exportRequest struct {
	OrgID   string
	GroupID string
	Types   string
	IP      string
	URL     string
	Since   *sql.NullTime
}

// JobServiceV1 is a struct that defines what the service has
type JobServiceV1 struct {
	jr        v1Repo.JobRepo
	or        repository.OrganizationRepo
	gr        repository.GroupRepo
	bfdClient client.APIClient
}

// NewJobService function that creates and returns a JobService
func NewJobService(jr v1Repo.JobRepo, or repository.OrganizationRepo, gr repository.GroupRepo, bfdClient client.APIClient) *JobServiceV1 {
	return &JobServiceV1{
		jr,
		or,
		gr,
		bfdClient,
	}
}

// Export function that starts an export job for a given Group ID using v1 db
func (js JobServiceV1) Export(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())

	ei := getExportRequest(w, r)
	if ei == nil {
		return
	}

	g, err := js.gr.FindByID(r.Context(), ei.GroupID)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to fetch the group %s", ei.GroupID), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	m, err := g.GetAttributionInfo()
	if err != nil {
		log.Error(fmt.Sprintf("Failed to retrieve member provider npi and patient npi from %s", ei.GroupID), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	o, err := js.or.FindByID(r.Context(), ei.OrgID)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to fetch the organization %s during group export %s", ei.OrgID, ei.GroupID), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	orgNPI, err := o.GetNPI()
	if err != nil {
		log.Error(fmt.Sprintf("Failed to retrieve NPI for organization %s during group export %s", ei.OrgID, ei.GroupID), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	npis := groupByProviderNPI(m)
	jobIDs, err := js.buildJobs(r.Context(), ei, orgNPI, npis)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to create jobs for group %s", ei.GroupID), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	b, err := json.Marshal(&exportResponse{JobIDs: jobIDs})
	if err != nil {
		log.Error(fmt.Sprintf("Failed to convert to response during group export %s", ei.GroupID), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(b); err != nil {
		log.Error("Failed to write job ID to response", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}
}

func (js JobServiceV1) buildJobs(ctx context.Context, er *exportRequest, orgNPI string, mbisByProvider map[string][]string) ([]string, error) {
	log := logger.WithContext(ctx)
	tt, err := js.fetchTransactionTime()
	if err != nil {
		return nil, errors.Errorf("Failed to fetch Transaction Time from BFD: %s", err.Error())
	}
	jobs := make([]string, 0)
	for provider, mbis := range mbisByProvider {
		batches, err := buildV1Batches(er, &v1.ProviderOrg{
			OrgNPI:      orgNPI,
			ProviderNPI: provider,
		}, mbis, *tt)
		if err != nil {
			log.Error("Failed to build batches", zap.Error(err))
			continue
		}
		job, err := js.jr.Insert(ctx, er.OrgID, batches)
		if err != nil {
			log.Error("Failed to create job", zap.Error(err))
			continue
		}
		log.Info(fmt.Sprintf(
			"dpcMetric=jobCreated,jobId=%s,orgId=%s,groupId=%s,totalPatients=%x,resourcesRequested=%s",
			*job,
			er.OrgID,
			er.GroupID,
			len(mbis),
			strings.ReplaceAll(er.Types, ",", ";")),
		)
		jobs = append(jobs, *job)
	}
	return jobs, nil
}

func groupByProviderNPI(m []model.Attribution) map[string][]string {
	patientNPIs := make(map[string][]string)
	for _, member := range m {
		patientNPIs[member.ProviderNPI] = append(patientNPIs[member.ProviderNPI], member.PatientMBI)
	}
	return patientNPIs
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

func buildV1Batches(ei *exportRequest, groupNPIs *v1.ProviderOrg, patientMBIs []string, tt time.Time) ([]v1.BatchRequest, error) {
	priority := 5000
	if len(patientMBIs) == 1 {
		priority = 1000
	}
	patientBatches := batchPatientMBIs(patientMBIs, conf.GetAsInt("queue.batchSize", 100))
	var batches []v1.BatchRequest
	for _, batchedPatients := range patientBatches {
		batch := createBatch(ei, groupNPIs, batchedPatients, len(patientMBIs) > 1)
		batch.Priority = priority
		if ei.Since.Valid && !tt.After(ei.Since.Time) {
			batch.PatientMBIs = ""
		}
		batch.TransactionTime = tt
		batches = append(batches, batch)
	}
	return batches, nil
}

func createBatch(ei *exportRequest, g *v1.ProviderOrg, p []string, b bool) v1.BatchRequest {
	return v1.BatchRequest{
		Since:           ei.Since,
		RequestURL:      ei.URL,
		RequestingIP:    ei.IP,
		OrganizationNPI: g.OrgNPI,
		ProviderNPI:     g.ProviderNPI,
		PatientMBIs:     strings.Join(p, ","),
		IsBulk:          b,
		ResourceTypes:   ei.Types,
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

func getExportRequest(w http.ResponseWriter, r *http.Request) *exportRequest {
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
	return &exportRequest{
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
