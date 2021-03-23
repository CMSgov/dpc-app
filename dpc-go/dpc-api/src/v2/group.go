package v2

import (
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"

	"github.com/CMSgov/dpc/api/client"
	"github.com/google/fhir/go/jsonformat"
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
func (oc *GroupController) Create(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	log := logger.WithContext(r.Context())

	if err := isValidGroup(body); err != nil {
		log.Error("Group is not valid in request", zap.Error(err))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Not a valid group")
		return
	}

	resp, err := oc.ac.Post(r.Context(), client.Group, body)
	if err != nil {
		log.Error("Failed to save the org to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save group")
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save group")
	}
}

func isValidGroup(group []byte) error {
	unmarshaller, _ := jsonformat.NewUnmarshaller("UTC", jsonformat.R4)
	_, err := unmarshaller.UnmarshalR4(group)

	//need to check structure here, i.e members are patient resources with mbi and managing practitioner
	if err != nil {
		return err
	}
	return nil
}
