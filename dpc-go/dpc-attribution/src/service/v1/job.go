package service

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/CMSgov/dpc/attribution/service"
	"github.com/CMSgov/dpc/attribution/util"
	"github.com/darahayes/go-boom"
	uuid "github.com/jackc/pgx/pgtype/ext/gofrs-uuid"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/logger"
	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	modelV1 "github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/CMSgov/dpc/attribution/repository"
)

// JobServiceV1 is a struct that defines what the service has
type JobServiceV1 struct {
	pr repository.PatientRepo
	jr repository.JobRepo
}

// NewJobService function that creates and returns a JobService
func NewJobService(ctx context.Context) service.JobService {
	log := logger.WithContext(ctx)
	attrDbV1 := repository.GetAttributionV1DbConnection()
	queueDbV1 := repository.GetQueueDbConnection()

	defer func() {
		if err := attrDbV1.Close(); err != nil {
			log.Fatal("Failed to close attribution v1 db connection", zap.Error(err))
		}
		if err := queueDbV1.Close(); err != nil {
			log.Fatal("Failed to close queue v1 db connection", zap.Error(err))
		}
	}()
	pr := repository.NewPatientRepo(attrDbV1)
	jr := repository.NewJobRepo(queueDbV1)
	return &JobServiceV1{
		pr,
		jr,
	}
}

// Export function that starts an export job for a given Group ID using v1 db
func (js *JobServiceV1) Export(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())

	groupID := util.FetchValueFromContext(r.Context(), w, middleware2.ContextKeyGroup)
	orgID := util.FetchValueFromContext(r.Context(), w, middleware2.ContextKeyOrganization)
	requestingIP := util.FetchValueFromContext(r.Context(), w, middleware2.ContextKeyRequestingIP)

	patientMBIs, err := js.pr.FindMBIsByGroupID(groupID)
	if err != nil {
		log.Error("Failed to fetch patients for group", zap.Error(err))
		boom.BadRequest(w,"The group must contain active patients")
		return
	}

	log.Info(fmt.Sprintf("Exporting data for group: %s _since: %s", groupID, ""))

	groupNPIs, err := js.jr.GetNPIs(r.Context(), groupID)
	if err != nil {
		log.Error("Failed to retrieve NPIs for Group", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	// TODO: break patients into batches
	batch := js.createNewJobBatch(orgID, groupNPIs, patientMBIs, requestingIP)

	jobID, err := js.jr.Insert(r.Context(), batch)
	if err != nil {
		log.Error("Failed to create job", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	jobBytes := new(bytes.Buffer)
	if err := json.NewEncoder(jobBytes).Encode(jobID); err != nil {
		log.Error("Failed to convert orm model to bytes for job", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(jobBytes.Bytes()); err != nil {
		log.Error("Failed to write job ID to response", zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

func (js *JobServiceV1) createNewJobBatch(orgID string, g *modelV1.GroupNPIs, patientMBIs []string, requestingIP string) modelV1.JobQueueBatch {
	return modelV1.JobQueueBatch{
		JobID:           uuid.UUID{},
		OrganizationID:  orgID,
		OrganizationNPI: g.OrgNPI,
		ProviderNPI:     g.ProviderNPI,
		PatientMBIs:     strings.Join(patientMBIs, ","),
		ResourceTypes:   "",
		Since:           time.Time{},
		TransactionTime: time.Time{},
		SubmitTime:      time.Time{},
		RequestingIP:    requestingIP,
		IsBulk:          true,
	}
}
