package v2

import (
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
)

// ImplementerController is a struct that defines what the controller has
type ImplementerController struct {
	ac client.Client
}

// NewImplementerController function that creates an implementer and returns its reference
func NewImplementerController(ac client.Client) *ImplementerController {
	return &ImplementerController{
		ac,
	}
}

// Create function is used for creating a new implementer by proxying request to dpc attribution
//goland:noinspection GoUnusedParameter
func (ic *ImplementerController) Create(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	log := logger.WithContext(r.Context())

	if len(body) == 0 {
		log.Error("Implementer body is empty")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Body is required")
		return
	}

	resp, err := ic.ac.Post(r.Context(), client.Implementer, body)
	if err != nil {
		log.Error("Failed to save the implementer to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to save implementer")
		return
	}

	if _, err := w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to save implementer")
	}
}

// Export function is not currently used for ImplementerController
//goland:noinspection GoUnusedParameter
func (ic *ImplementerController) Export(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Read function is not currently used for ImplementerController
//goland:noinspection GoUnusedParameter
func (ic *ImplementerController) Read(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Delete function is not currently used for ImplementerController
//goland:noinspection GoUnusedParameter
func (ic *ImplementerController) Delete(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Update function is not currently used for ImplementerController
//goland:noinspection GoUnusedParameter
func (ic *ImplementerController) Update(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}
