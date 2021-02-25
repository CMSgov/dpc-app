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

const ContextKeyOrganization contextKey = iota

func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), ContextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

type OrganizationService struct {
	repo repository.OrganizationRepo
}

func NewOrganizationService(repo repository.OrganizationRepo) *OrganizationService {
	return &OrganizationService{
		repo,
	}
}

func (os *OrganizationService) Get(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract organization id from context")
		boom.BadRequest(w, "Could not get organization id")
		return
	}

	log.Info(fmt.Sprintf("Retriving organization with id %s", organizationID))

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

func (os *OrganizationService) Post(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	body, _ := ioutil.ReadAll(r.Body)

	log.Info(fmt.Sprintf("Saving organization"))

	org, err := os.repo.Create(r.Context(), body)
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

func (os *OrganizationService) Delete(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract organization id from context")
		boom.BadRequest(w, "Could not get organization id")
		return
	}

	log.Info(fmt.Sprintf("Deleting organization with id %s", organizationID))

	err := os.repo.DeleteByID(r.Context(), organizationID)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to find organization to delete"), zap.Error(err))
		boom.NotFound(w, err.Error())
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

func (os *OrganizationService) Put(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract organization id from context")
		boom.BadRequest(w, "Could not get organization id")
		return
	}

	body, _ := ioutil.ReadAll(r.Body)

	log.Info(fmt.Sprintf("Updating organization %s", organizationID))

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
