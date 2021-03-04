package v2

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/darahayes/go-boom"
	"github.com/go-chi/chi"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
)

type contextKey int

// ContextKeyOrganization is the key in the context to retrieve the organizationID
const ContextKeyOrganization contextKey = iota

// OrganizationCtx middleware to extract the organizationID from the chi url param and set it into the request context
func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), ContextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// OrganizationService is a struct that defines what the service has
type OrganizationService struct {
	repo repository.OrganizationRepo
}

// NewOrganizationService function that creates a organization service and returns it's reference
func NewOrganizationService(repo repository.OrganizationRepo) *OrganizationService {
	return &OrganizationService{
		repo,
	}
}

// Get function that get the organization from the database by id and logs any errors before returning a generic error
func (os *OrganizationService) Get(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract organization id from context")
		boom.BadRequest(w, "Could not get organization id")
		return
	}

	org, err := os.repo.FindByID(r.Context(), organizationID)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to retrieve organization"), zap.Error(err))
		boom.NotFound(w, err.Error())
		return
	}

	orgBytes := new(bytes.Buffer)
	if err := json.NewEncoder(orgBytes).Encode(org); err != nil {
		log.Error(fmt.Sprintf("Failed to convert orm model to bytes for organization"), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(orgBytes.Bytes()); err != nil {
		log.Error(fmt.Sprintf("Failed to write organization to response for organization"), zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

// Post function that saves the organization to the database and logs any errors before returning a generic error
func (os *OrganizationService) Post(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	body, _ := ioutil.ReadAll(r.Body)

	org, err := os.repo.Insert(r.Context(), body)
	if err != nil {
		log.Error("Failed to create organization", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	orgBytes := new(bytes.Buffer)
	if err := json.NewEncoder(orgBytes).Encode(org); err != nil {
		log.Error(fmt.Sprintf("Failed to convert orm model to bytes for organization"), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(orgBytes.Bytes()); err != nil {
		log.Error(fmt.Sprintf("Failed to write organization to response for organization"), zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

// Delete function that deletes the organization to the database and logs any errors before returning a generic error
func (os *OrganizationService) Delete(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract organization id from context")
		boom.BadRequest(w, "Could not get organization id")
		return
	}

	err := os.repo.DeleteByID(r.Context(), organizationID)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to find organization to delete"), zap.Error(err))
		boom.NotFound(w, err.Error())
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// Put function that updates the organization in the database and logs any errors before returning a generic error
func (os *OrganizationService) Put(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract organization id from context")
		boom.BadRequest(w, "Could not get organization id")
		return
	}

	body, _ := ioutil.ReadAll(r.Body)

	org, err := os.repo.Update(r.Context(), organizationID, body)
	if err != nil {
		log.Error("Failed to update organization", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	orgBytes := new(bytes.Buffer)
	if err := json.NewEncoder(orgBytes).Encode(org); err != nil {
		log.Error(fmt.Sprintf("Failed to convert orm model to bytes for organization"), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	if _, err := w.Write(orgBytes.Bytes()); err != nil {
		log.Error(fmt.Sprintf("Failed to write organization to response for organization"), zap.Error(err))
		boom.Internal(w, err.Error())
	}
}
