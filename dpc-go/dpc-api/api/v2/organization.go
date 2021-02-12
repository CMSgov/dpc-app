package v2

import (
	"context"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/pkg/errors"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"

	"github.com/go-chi/chi"

	"github.com/CMSgov/dpc/api/client"
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

type OrganizationController struct {
	ac client.Client
}

func NewOrganizationController(ac client.Client) *OrganizationController {
	return &OrganizationController{
		ac,
	}
}

func (oc *OrganizationController) Read(w http.ResponseWriter, r *http.Request) {
	organizationID, ok := r.Context().Value(ContextKeyOrganization).(string)
	log := logger.WithContext(r.Context())
	if !ok {
		log.Error("Failed to extract the organization id from the context")
		fhirror.BusinessViolation(w, r.Context(), http.StatusBadRequest, "Failed to extract organization id from url, please check the url")
		return
	}

	resp, err := oc.ac.Get(r.Context(), client.Organization, organizationID)
	if err != nil {
		log.Error("Failed to get the org from attribution", zap.Error(err))
		fhirror.ServerIssue(w, r.Context(), http.StatusNotFound, "Failed to find organization")
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(w, r.Context(), http.StatusNotFound, "Failed to find organization")
	}
}

func (oc *OrganizationController) Create(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	log := logger.WithContext(r.Context())

	if err := isValidOrganization(body); err != nil {
		log.Error("Organization is not valid in request", zap.Error(err))
		fhirror.BusinessViolation(w, r.Context(), http.StatusBadRequest, "Not a valid organization")
		return
	}

	resp, err := oc.ac.Post(r.Context(), client.Organization, body)
	if err != nil {
		log.Error("Failed to save the org to attribution", zap.Error(err))
		fhirror.ServerIssue(w, r.Context(), http.StatusUnprocessableEntity, "Failed to save the organization")
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(w, r.Context(), http.StatusUnprocessableEntity, "Failed to save organization")
	}
}

func isValidOrganization(org []byte) error {
	_, err := fhir.UnmarshalOrganization(org)
	if err != nil {
		return errors.New("Organization is not a properly formed FHIR object")
	}
	return nil
}
