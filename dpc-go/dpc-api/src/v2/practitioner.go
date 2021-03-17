package v2

import (
	"context"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"

	"github.com/go-chi/chi"

	"github.com/CMSgov/dpc/api/client"
	"github.com/google/fhir/go/jsonformat"
)

// ContextKeyPractitioner is the key in the context to retrieve the practitionerID
const ContextKeyPractitioner contextKey = iota

// PractitionerCtx middleware to extract the practitionerID from the chi url param and set it into the request context
func PractitionerCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		practitionerID := chi.URLParam(r, "practitionerID")
		ctx := context.WithValue(r.Context(), ContextKeyPractitioner, practitionerID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// PractitionerController is a struct that defines what the controller has
type PractitionerController struct {
	ac client.Client
}

// NewPractitionerController function that creates a practitioner controller and returns its reference
func NewPractitionerController(ac client.Client) *PractitionerController {
	return &PractitionerController{
		ac,
	}
}

// Read function that calls attribution service via get to return the practitioner specified by practitionerID
func (pc *PractitionerController) Read(w http.ResponseWriter, r *http.Request) {
	practitionerID, ok := r.Context().Value(ContextKeyPractitioner).(string)
	log := logger.WithContext(r.Context())
	if !ok {
		log.Error("Failed to extract the practitioner id from the context")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to extract practitioner id from url, please check the url")
		return
	}

	resp, err := pc.ac.Get(r.Context(), client.Practitioner, practitionerID)
	if err != nil {
		log.Error("Failed to get the practitioner from attribution", zap.Error(err))
		fhirror.NotFound(r.Context(), w, "Failed to find practitioner")
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.NotFound(r.Context(), w, "Failed to find practitioner")
	}
}

// Create function that calls attribution service via post to save a practitioner into attribution service
func (pc *PractitionerController) Create(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	log := logger.WithContext(r.Context())

	if err := isValidPractitioner(body); err != nil {
		log.Error("Practitioner is not valid in request", zap.Error(err))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Not a valid practitioner")
		return
	}

	resp, err := pc.ac.Post(r.Context(), client.Practitioner, body)
	if err != nil {
		log.Error("Failed to save the org to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save practitioner")
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save practitioner")
	}
}

// Delete function that calls attribution service via delete to delete a practitioner from attribution services
func (pc *PractitionerController) Delete(w http.ResponseWriter, r *http.Request) {
	practitionerID, ok := r.Context().Value(ContextKeyPractitioner).(string)
	log := logger.WithContext(r.Context())
	if !ok {
		log.Error("Failed to extract the practitioner id from the context")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to extract practitioner id from url, please check the url")
		return
	}

	err := pc.ac.Delete(r.Context(), client.Practitioner, practitionerID)
	if err != nil {
		log.Error("Failed to save the practitioner to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to save the practitioner")
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// Update function that calls attribution service via put to update an practitioner in attribution service
func (pc *PractitionerController) Update(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	practitionerID, ok := r.Context().Value(ContextKeyPractitioner).(string)
	if !ok {
		log.Error("Failed to extract the practitioner id from the context")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to extract practitioner id from url, please check the url")
		return
	}

	body, _ := ioutil.ReadAll(r.Body)

	if err := isValidPractitioner(body); err != nil {
		log.Error("Practitioner is not valid in request", zap.Error(err))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Not a valid practitioner")
		return
	}

	resp, err := pc.ac.Put(r.Context(), client.Practitioner, practitionerID, body)
	if err != nil {
		log.Error("Failed to update the practitioner to attribution", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to update the practitioner")
		return
	}

	if _, err = w.Write(resp); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusUnprocessableEntity, "Failed to update practitioner")
	}
}

func isValidPractitioner(practitioner []byte) error {
	unmarshaller, _ := jsonformat.NewUnmarshaller("UTC", jsonformat.R4)
	_, err := unmarshaller.UnmarshalR4(practitioner)
	if err != nil {
		return err
	}
	return nil
}
