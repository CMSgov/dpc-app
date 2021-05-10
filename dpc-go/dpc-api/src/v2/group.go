package v2

import (
	"encoding/json"
	"io/ioutil"
	"net/http"

	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/model"
	"github.com/google/fhir/go/jsonformat"
	"github.com/pkg/errors"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/api/client"
)

// GroupController is a struct that defines what the controller has
type GroupController struct {
	ac client.Client
}

// NewGroupController function that creates a organization controller and returns it's reference
func NewGroupController(ac client.Client) *GroupController {
	return &GroupController{
		ac,
	}
}

// Create function that calls attribution service via post to save an organization into attribution service
func (gc *GroupController) Create(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	log := logger.WithContext(r.Context())

	if err := isValidGroup(body); err != nil {
		log.Error("Group is not valid in request", zap.Error(err))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Not a valid group")
		return
	}

	resp, err := gc.ac.Post(r.Context(), client.Group, body)
	if err != nil {
		log.Error("Failed to save the org to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save group")
		return
	}

	if _, err := w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save group")
	}

}

// Read function is not currently used for GroupController
func (gc *GroupController) Read(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusMethodNotAllowed)
}

// Delete function is not currently used for GroupController
func (gc *GroupController) Delete(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusMethodNotAllowed)
}

// Update function is not currently used for GroupController
func (gc *GroupController) Update(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusMethodNotAllowed)
}

// Export function is not currently used for GroupController
func (gc *GroupController) Export(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusMethodNotAllowed)
}

func isValidGroup(group []byte) error {
	unmarshaller, _ := jsonformat.NewUnmarshaller("UTC", jsonformat.R4)
	_, err := unmarshaller.Unmarshal(group)
	if err != nil {
		return err
	}

	var groupStruct model.Group
	if err := json.Unmarshal(group, &groupStruct); err != nil {
		return err
	}

	for _, m := range groupStruct.Member {
		pracRef := findPractitionerRef(m)
		if pracRef == nil ||
			pracRef.Identifier == nil ||
			*pracRef.Type != "Practitioner" {
			return errors.New("Should contain a provider identifier")
		}
		patientRef := m.Entity
		if patientRef == nil ||
			patientRef.Identifier == nil ||
			*patientRef.Type != "Patient" {
			return errors.New("Should contain a patient identifier")
		}
	}
	return nil
}

// at this point there should only be one extension because of fhir_filter,
// but going to leave this in to still search the extensions
func findPractitionerRef(member model.GroupMember) *fhir.Reference {
	for _, e := range member.Extension {
		vr := e.ValueReference
		if vr != nil && vr.Type != nil && *vr.Type == "Practitioner" {
			return vr
		}
	}
	return nil
}
