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

type contextKeyString string

// ContextKeyIP is the key in the context to store the requesting IP
const ContextKeyIP contextKeyString = ""

// GroupCtx middleware to extract the groupID from the chi url param and set it into the request context
func GroupCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		groupID := chi.URLParam(r, "groupID")
		ctx := context.WithValue(r.Context(), ContextKeyGroup, groupID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

//GroupService is a struct that defines what the service has
type GroupService struct {
	js service.JobService
}

// NewGroupService function that creates a group service and returns its reference
func NewGroupService(js service.JobService) *GroupService {
	return &GroupService{
		js,
	}
}

// Export function that starts an export job for a given Group ID
func (gs *GroupService) Export(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
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
	// TODO: handle Type query param add to ctx
	// TODO: handle _since query param add to ctx
	// TODO: handle transaction time
	ctx := gs.setRequestingIP(r)
	log.Info("Exporting data for group: {} _since: {}")
	gs.js.Export(w http.ResponseWriter, ctx, orgID, groupID)
}

func (gs *GroupService) setRequestingIP(r *http.Request) context.Context {
	ipAddress := r.Header.Get("X-Forwarded-For")
	if ipAddress == "" {
		ipAddress = r.RemoteAddr
	}
	return context.WithValue(r.Context(), ContextKeyIP, ipAddress)
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
