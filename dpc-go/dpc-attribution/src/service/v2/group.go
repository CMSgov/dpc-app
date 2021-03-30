package v2

import (
	"context"
	"net/http"

	"github.com/darahayes/go-boom"
	"github.com/go-chi/chi"

	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/service"
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
	js service.Job
}

// NewGroupService function that creates a group service and returns its reference
func NewGroupService(js service.Job) *GroupService {
	return &GroupService{
		js,
	}
}

// Export function that starts an export job for a given Group ID
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
	gs.js.Export(r.Context(), orgID, groupID)
}

// Get function is not currently used for v2.GroupService
func (gs *GroupService) Get(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusMethodNotAllowed)
}

// Post function is not currently used for v2.GroupService
func (gs *GroupService) Post(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusMethodNotAllowed)
}

// Delete function is not currently used for v2.GroupService
func (gs *GroupService) Delete(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusMethodNotAllowed)
}

// Put function is not currently used for v2.GroupService
func (gs *GroupService) Put(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusMethodNotAllowed)
}
