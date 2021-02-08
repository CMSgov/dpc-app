package v2

import (
	"context"
	"github.com/CMSgov/dpc/api/pkg/fhirror"
	"github.com/pkg/errors"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"io/ioutil"
	"net/http"

	"github.com/go-chi/chi"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/api/pkg/client"
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
	if !ok {
		zap.L().Error("Failed to extract the organization id from the context")
		fhirror.BusinessViolation(w, http.StatusBadRequest, "Failed to extract organization id from url, please check the url")
		return
	}

	resp, err := organizationController.attributionClient.Get(client.Organization, organizationID)
	if err != nil {
		zap.L().Error("Failed to get the org from attribution", zap.Error(err))
		fhirror.ServerIssue(w, http.StatusNotFound, "Failed to find organization")
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if _, err = w.Write(resp); err != nil {
		zap.L().Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(w, http.StatusNotFound, "Failed to find organization")
	}
}

func (organizationController *OrganizationController) CreateOrganization(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	if err := isValidOrganization(body); err != nil {
		zap.L().Error("Organization is not valid in request")
		fhirror.BusinessViolation(w, http.StatusBadRequest, "Not a valid organization")
		return
	}

	resp, err := organizationController.attributionClient.Post(client.Organization, body)
	if err != nil {
		zap.L().Error("Failed to save the org to attribution", zap.Error(err))
		fhirror.ServerIssue(w, http.StatusUnprocessableEntity, "Failed to save the organization")
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if _, err = w.Write(resp); err != nil {
		zap.L().Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(w, http.StatusUnprocessableEntity, "Failed to save organization")
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}

func isValidOrganization(org []byte) error {
	_, err := fhir.UnmarshalOrganization(org)
	if err != nil {
		return errors.New("Organization is not a properly formed FHIR object")
	}
	return nil
}
