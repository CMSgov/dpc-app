package v2

import (
	"context"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/pkg/errors"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"

	"github.com/go-chi/chi"

	"github.com/CMSgov/dpc/api/client"
	"github.com/google/fhir/go/jsonformat"
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

// OrganizationController is a struct that defines what the controller has
type OrganizationController struct {
	ac client.Client
}

// NewOrganizationController function that creates a organization controller and returns it's reference
func NewOrganizationController(ac client.Client) *OrganizationController {
	return &OrganizationController{
		ac,
	}
}

// Read function that calls attribution service via get to return the organization specified by organizationID
func (oc *OrganizationController) Read(w http.ResponseWriter, r *http.Request) {
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	log := logger.WithContext(r.Context())
	if !ok {
		log.Error("Failed to extract the organization id from the context")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to extract organization id from url, please check the url")
		return
	}

	resp, err := oc.ac.Get(r.Context(), client.Organization, organizationID)
	if err != nil {
		log.Error("Failed to get the org from attribution", zap.Error(err))
		fhirror.NotFound(r.Context(), w, "Failed to find organization")
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.NotFound(r.Context(), w, "Failed to find organization")
	}
}

// Create function that calls attribution service via post to save an organization into attribution service
func (oc *OrganizationController) Create(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	log := logger.WithContext(r.Context())

	if err := isValidOrganization(body); err != nil {
		log.Error("Organization is not valid in request", zap.Error(err))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Not a valid organization")
		return
	}

	resp, err := oc.ac.Post(r.Context(), client.Organization, body)
	if err != nil {
		log.Error("Failed to save the org to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save organization")
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save organization")
	}
}

// Delete function that calls attribution service via delete to delete an organization from attribution services
func (oc *OrganizationController) Delete(w http.ResponseWriter, r *http.Request) {
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	log := logger.WithContext(r.Context())
	if !ok {
		log.Error("Failed to extract the organization id from the context")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to extract organization id from url, please check the url")
		return
	}

	err := oc.ac.Delete(r.Context(), client.Organization, organizationID)
	if err != nil {
		log.Error("Failed to save the org to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save the organization")
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// Update function that calls attribution service via put to update an organization in attribution service
func (oc *OrganizationController) Update(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract the organization id from the context")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to extract organization id from url, please check the url")
		return
	}

	body, _ := ioutil.ReadAll(r.Body)

	if err := isValidOrganization(body); err != nil {
		log.Error("Organization is not valid in request", zap.Error(err))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Not a valid organization")
		return
	}

	resp, err := oc.ac.Put(r.Context(), client.Organization, organizationID, body)
	if err != nil {
		log.Error("Failed to update the org to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to update the organization")
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to update organization")
	}
}

func isValidOrganization(org []byte) error {
	unmarshaller, _ := jsonformat.NewUnmarshaller("UTC", jsonformat.R4)
	_, err := unmarshaller.UnmarshalR4(org)
	if err != nil {
		return errors.New("Organization is not a properly formed FHIR object")
	}
	return nil
}
