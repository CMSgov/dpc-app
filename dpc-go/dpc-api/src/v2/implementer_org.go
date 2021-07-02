package v2

import (
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

    resp, err := ioc.ac.Post(r.Context(), client.ImplementerOrg, body)
    if err != nil {
        log.Error("Failed to save the implementer/org relationship to attribution", zap.Error(err))
        fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to save implementer/org relationship")
        return
    }

    if _, err := w.Write(resp); err != nil {
        log.Error("Failed to write data to response", zap.Error(err))
        fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to save implementer/org relationship")
    }
}

// Export function is not currently used for ImplementerOrgController
//goland:noinspection GoUnusedParameter
func (ioc *ImplementerOrgController) Export(w http.ResponseWriter, r *http.Request) {
    w.WriteHeader(http.StatusNotImplemented)
}

// Read function is not currently used for ImplementerOrgController
//goland:noinspection GoUnusedParameter
func (ioc *ImplementerOrgController) Read(w http.ResponseWriter, r *http.Request) {
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
