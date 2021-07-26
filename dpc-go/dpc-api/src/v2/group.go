package v2

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"

	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/model"
	"github.com/google/fhir/go/jsonformat"
	"github.com/pkg/errors"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"github.com/sjsdfg/common-lang-in-go/StringUtils"
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
		log.Error("Failed to save the group to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save group")
		return
	}

	if _, err := w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save group")
	}

}

// Export function that calls attribution service via get in order to start a job for data export
func (gc *GroupController) Export(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	groupID, ok := r.Context().Value(middleware2.ContextKeyGroup).(string)
	if !ok {
		log.Error("Failed to extract the group id from the context")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to extract group id from url, please check the url")
		return
	}

	outputFormat := r.URL.Query().Get("_outputFormat")
	if err := isValidExport(r.Context(), w, outputFormat, r.Header.Get("Prefer")); err != nil {
		return
	}
	resp, err := gc.ac.Export(r.Context(), client.Group, groupID)
	if err != nil {
		log.Error("Failed to start the job in attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, err.Error())
		return
	}

	contentLocation := contentLocationHeader(string(resp), r)
	w.Header().Set("Content-Location", contentLocation)
	w.WriteHeader(http.StatusAccepted)
}

func contentLocationHeader(id string, r *http.Request) string {
	scheme := "http"
	if r.TLS != nil {
		scheme = "https"
	}
	return fmt.Sprintf("%s://%s/v2/Jobs/%s", scheme, r.Host, id)
}

// Read function is not currently used for GroupController
//goland:noinspection GoUnusedParameter
func (gc *GroupController) Read(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Delete function is not currently used for GroupController
//goland:noinspection GoUnusedParameter
func (gc *GroupController) Delete(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Update function is not currently used for GroupController
//goland:noinspection GoUnusedParameter
func (gc *GroupController) Update(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
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

func isValidExport(ctx context.Context, w http.ResponseWriter, outputFormat string, headerPrefer string) error {
	log := logger.WithContext(ctx)
	// _outputFormat only supports FhirNdjson, ApplicationNdjson, Ndjson
	if StringUtils.EqualsNoneIgnoreCase(outputFormat, middleware2.FhirNdjson, middleware2.ApplicationNdjson, middleware2.Ndjson) {
		log.Error("Invalid outputFormat")
		fhirror.BusinessViolation(ctx, w, http.StatusBadRequest, "'_outputFormat' query parameter must be 'application/fhir+ndjson', 'application/ndjson', or 'ndjson'")
	}
	if StringUtils.IsEmpty(headerPrefer) {
		log.Error("Missing Prefer header")
		fhirror.BusinessViolation(ctx, w, http.StatusBadRequest, "The 'Prefer' header is required and must be 'respond-async'")
	}
	if StringUtils.IsNotEmpty(headerPrefer) && headerPrefer != "respond-async" {
		log.Error("Invalid Prefer header")
		fhirror.BusinessViolation(ctx, w, http.StatusBadRequest, "The 'Prefer' header must be 'respond-async'")
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
