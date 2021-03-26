package v2

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/darahayes/go-boom"
	"github.com/go-chi/chi"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
)

// ContextKeyGroup is the key in the context to retrieve the groupID
const ContextKeyGroup contextKey = iota

// GroupCtx middleware to extract the groupID from the chi url param and set it into the request context
func GroupCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		groupID := chi.URLParam(r, "groupID")
		ctx := context.WithValue(r.Context(), ContextKeyGroup, groupID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// GroupService is a struct that defines what the service has
type GroupService struct {
	repo repository.GroupRepo
	pr   repository.PatientRepo
	jr   repository.JobRepo
}

// NewGroupService function that creates a group service and returns it's reference
func NewGroupService(repo repository.GroupRepo, pr repository.PatientRepo, jr repository.JobRepo) *GroupService {
	return &GroupService{
		repo,
		pr,
		jr,
	}
}

// Get function that gets the group from the database by group id and org id and logs any errors before returning a generic error
func (gs *GroupService) Get(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	groupID, ok := r.Context().Value(ContextKeyGroup).(string)
	if !ok {
		log.Error("Failed to extract group id from context")
		boom.BadRequest(w, "Could not get group id")
		return
	}

	org, err := gs.repo.FindByID(r.Context(), groupID)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to retrieve group"), zap.Error(err))
		boom.NotFound(w, err.Error())
		return
	}

	orgBytes := new(bytes.Buffer)
	if err := json.NewEncoder(orgBytes).Encode(org); err != nil {
		log.Error(fmt.Sprintf("Failed to convert orm model to bytes for group"), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(orgBytes.Bytes()); err != nil {
		log.Error(fmt.Sprintf("Failed to write group to response for group"), zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

// Export function that starts an export job for a given Group ID using v1 db
func (gs *GroupService) Export(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	log.Info("Exporting data for group: {} _since: {}")
	groupID, ok := r.Context().Value(ContextKeyGroup).(string)
	if !ok {
		log.Error("Failed to extract group id from context")
		boom.BadRequest(w, "Could not get group id")
		return
	}
	orgID, ok := r.Context().Value(ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract org id from context")
		boom.BadRequest(w, "Could not get org id")
		return
	}
	patientMBIs, err := gs.pr.FindMBIsByGroupID(groupID)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to fetch patients for group"), zap.Error(err))
		boom.Internal(w, err.Error())
	}

	// TODO: handle Type query param
	// TODO: handle _since query param
	// TODO: set requesting IP address
	// TODO: return a job UUID from the create job func (start job)
	gs.jr.Insert()

}
