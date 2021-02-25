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

/*
   OrganizationCtx
   middleware to extract the organizationID from the chi url param and set it into the request context
*/
func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), ContextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

type organizationService struct {
	repo repository.OrganizationRepo
}

/*
   NewOrganizationService
   function that creates a organization service and returns it's reference

*/
func NewOrganizationService(repo repository.OrganizationRepo) *organizationService {
	return &organizationService{
		repo,
	}
}

/*
   Get
   function that get the organization from the database by id and logs any errors before returning a generic error
*/
func (os *organizationService) Get(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract organization id from context")
		boom.BadData(w, "Could not get organization id")
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

/*
   Save
   function that saves the organization to the database and logs any errors before returning a generic error
*/
func (os *organizationService) Save(w http.ResponseWriter, r *http.Request) {
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
