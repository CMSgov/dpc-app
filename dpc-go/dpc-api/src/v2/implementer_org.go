package v2

import (
	"encoding/json"
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
)

// ImplementerOrgController is a struct that defines what the controller has
type ImplementerOrgController struct {
	ac client.Client
}

// NewImplementerOrgController creates an implementer org controller and returns its reference
func NewImplementerOrgController(ac client.Client) *ImplementerOrgController {
	return &ImplementerOrgController{
		ac,
	}
}

// Create function is used to create a relationship between the specified implementer and organization
func (ioc *ImplementerOrgController) Create(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	log := logger.WithContext(r.Context())

	if len(body) == 0 {
		log.Error("Implementer org body is empty")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Body is required")
		return
	}

	resp, err := ioc.ac.CreateImplOrg(r.Context(), body)
	if err != nil {
		log.Error("Failed to save the implementer/org relationship to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to save implementer/org relationship")
		return
	}

	b, _ := json.Marshal(resp)
	if err != nil {
		log.Error("Failed to convert implementer/org to bytes", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Internal server error")
		return
	}

	if _, err := w.Write(b); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Internal server error")
	}
}

// Read calls attribution service via GET to return the Organizations associated with an Implementer
func (ioc *ImplementerOrgController) Read(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())

	resp, err := ioc.ac.GetImplOrg(r.Context())
	if err != nil {
		log.Error("Failed to get the implementer organization(s) from attribution", zap.Error(err))
		fhirror.NotFound(r.Context(), w, "Failed to find implementer organization(s)")
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.NotFound(r.Context(), w, "Failed to find implementer organization(s)")
	}
}

// Export function is not currently used for ImplementerOrgController
//goland:noinspection GoUnusedParameter
func (ioc *ImplementerOrgController) Export(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Delete function is not currently used for ImplementerOrgController
//goland:noinspection GoUnusedParameter
func (ioc *ImplementerOrgController) Delete(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Update function is not currently used for ImplementerOrgController
//goland:noinspection GoUnusedParameter
func (ioc *ImplementerOrgController) Update(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}
