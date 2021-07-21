package v2

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/google/uuid"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
)

// ImplementerController is a struct that defines what the controller has
type ImplementerController struct {
	ac client.Client
	sc client.SsasClient
}

// NewImplementerController function that creates an implementer and returns its reference
func NewImplementerController(ac client.Client, sc client.SsasClient) *ImplementerController {
	return &ImplementerController{
		ac, sc,
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

	resBytes, err := ic.ac.Post(r.Context(), client.Implementer, body)
	if err != nil {
		log.Error("Failed to save the implementer to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to save implementer")
		return
	}
	impl := ImplementerResource{}
	if err := json.NewDecoder(bytes.NewReader(resBytes)).Decode(&impl); err != nil {
		log.Error("Failed to convert attribution response bytes to CreateImplAttrResponse model", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to create implementer")
		return
	}

	req := client.CreateGroupRequest{
		Name:    impl.Name,
		GroupID: uuid.New().String(),
		XData:   fmt.Sprintf("{\"implementerID\": \"%s\"}", impl.ID),
	}

	gResp, err := ic.sc.CreateGroup(r.Context(), req)
	if err != nil {
		//TODO we will need to add aditional logic to handle rollbacks
		log.Error("Failed to create ssas group", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to create implementer")
		return
	}

	//Update implementer with ssas group
	impl.SsasGroupId = gResp.GroupID
	reqBytes, err := json.Marshal(impl)
	if err != nil {
		log.Error("Failed to convert Implementer model to bytes", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to create implementer")
		return
	}

	if resBytes, err = ic.ac.Put(r.Context(), client.Implementer, impl.ID, reqBytes); err != nil {
		log.Error("Failed to save the implementer to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to save implementer")
		return
	}

	if _, err := w.Write(resBytes); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to create implementer")
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

// ImplementerResource struct that models an attribution implementer
type ImplementerResource struct {
	ID          string `json:"id" faker:"uuid_hyphenated"`
	Name        string `json:"name" faker:"word"`
	SsasGroupId string `json:"ssas_group_id,omitempty" faker:"word"`
}
