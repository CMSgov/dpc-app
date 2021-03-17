package v2

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/darahayes/go-boom"
	"github.com/go-chi/chi"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
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

// PractitionerService is a struct that defines what the service has
type PractitionerService struct {
	repo repository.PractitionerRepo
}

// NewPractitionerService function that creates a practitioner service and returns it's reference
func NewPractitionerService(repo repository.PractitionerRepo) *PractitionerService {
	return &PractitionerService{
		repo,
	}
}

// Get function that get the practitioner from the database by id and logs any errors before returning a generic error
func (ps *PractitionerService) Get(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	practitionerID, ok := r.Context().Value(ContextKeyPractitioner).(string)
	if !ok {
		log.Error("Failed to extract practitioner id from context")
		boom.BadRequest(w, "Could not get practitioner id")
		return
	}

	pract, err := ps.repo.FindByID(r.Context(), practitionerID)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to retrieve practitioner"), zap.Error(err))
		boom.NotFound(w, err.Error())
		return
	}

	practBytes := new(bytes.Buffer)
	if err := json.NewEncoder(practBytes).Encode(pract); err != nil {
		log.Error(fmt.Sprintf("Failed to convert orm model to bytes for practitioner"), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(practBytes.Bytes()); err != nil {
		log.Error(fmt.Sprintf("Failed to write practitioner to response for practitioner"), zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

// Post function that saves the practitioner to the database and logs any errors before returning a generic error
func (ps *PractitionerService) Post(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	body, _ := ioutil.ReadAll(r.Body)

	pract, err := ps.repo.Insert(r.Context(), body)
	if err != nil {
		log.Error("Failed to create practitioner", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	practBytes := new(bytes.Buffer)
	if err := json.NewEncoder(practBytes).Encode(pract); err != nil {
		log.Error(fmt.Sprintf("Failed to convert orm model to bytes for practitioner"), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(practBytes.Bytes()); err != nil {
		log.Error(fmt.Sprintf("Failed to write practitioner to response for practitioner"), zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

// Delete function that deletes the practitioner to the database and logs any errors before returning a generic error
func (ps *PractitionerService) Delete(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	practitionerID, ok := r.Context().Value(ContextKeyPractitioner).(string)
	if !ok {
		log.Error("Failed to extract practitioner id from context")
		boom.BadRequest(w, "Could not get practitioner id")
		return
	}

	err := ps.repo.DeleteByID(r.Context(), practitionerID)
	if err != nil {
		log.Error(fmt.Sprintf("Failed to find practitioner to delete"), zap.Error(err))
		boom.NotFound(w, err.Error())
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// Put function that updates the practitioner in the database and logs any errors before returning a generic error
func (ps *PractitionerService) Put(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	practitionerID, ok := r.Context().Value(ContextKeyPractitioner).(string)
	if !ok {
		log.Error("Failed to extract practitioner id from context")
		boom.BadRequest(w, "Could not get practitioner id")
		return
	}

	body, _ := ioutil.ReadAll(r.Body)

	pract, err := ps.repo.Update(r.Context(), practitionerID, body)
	if err != nil {
		log.Error("Failed to update practitioner", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	practBytes := new(bytes.Buffer)
	if err := json.NewEncoder(practBytes).Encode(pract); err != nil {
		log.Error(fmt.Sprintf("Failed to convert orm model to bytes for practitioner"), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	if _, err := w.Write(practBytes.Bytes()); err != nil {
		log.Error(fmt.Sprintf("Failed to write practitioner to response for practitioner"), zap.Error(err))
		boom.Internal(w, err.Error())
	}
}
