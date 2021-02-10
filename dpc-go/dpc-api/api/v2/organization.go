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

type contextKey string

func (c contextKey) String() string {
	return string(c)
}

var (
	contextKeyOrganization = contextKey("organization")
)

func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), contextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

type OrganizationController struct {
	attributionClient *client.AttributionClient
}

func NewOrganizationController(config *client.AttributionConfig) *OrganizationController {
	attributionClient := client.NewAttributionClient(config)
	return &OrganizationController{
		attributionClient: attributionClient,
	}
}

func (organizationController *OrganizationController) GetOrganization(w http.ResponseWriter, r *http.Request) {
	organizationID, ok := r.Context().Value(contextKeyOrganization).(string)
	log := logger.WithContext(r.Context())
	if !ok {
		log.Error("Failed to extract the organization id from the context")
		fhirror.BusinessViolation(w, r.Context(), http.StatusBadRequest, "Failed to extract organization id from url, please check the url")
		return
	}

	resp, err := organizationController.attributionClient.Get(r.Context(), client.Organization, organizationID)
	if err != nil {
		log.Error("Failed to get the org from attribution", zap.Error(err))
		fhirror.ServerIssue(w, r.Context(), http.StatusNotFound, "Failed to find organization")
		return
	}

	//w.Header().Set("Content-Type", "application/fhir+json")
	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(w, r.Context(), http.StatusNotFound, "Failed to find organization")
	}
}

func (organizationController *OrganizationController) CreateOrganization(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	log := logger.WithContext(r.Context())

	if err := isValidOrganization(body); err != nil {
		log.Error("Organization is not valid in request")
		fhirror.BusinessViolation(w, r.Context(), http.StatusBadRequest, "Not a valid organization")
		return
	}

	resp, err := organizationController.attributionClient.Post(r.Context(), client.Organization, body)
	if err != nil {
		log.Error("Failed to save the org to attribution", zap.Error(err))
		fhirror.ServerIssue(w, r.Context(), http.StatusUnprocessableEntity, "Failed to save the organization")
		return
	}

	//w.Header().Set("Content-Type", "application/fhir+json")
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
