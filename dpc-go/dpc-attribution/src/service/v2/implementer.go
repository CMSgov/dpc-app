package v2

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/middleware"
	v2 "github.com/CMSgov/dpc/attribution/model/v2"
	"io/ioutil"
	"net/http"

	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/darahayes/go-boom"
	"go.uber.org/zap"
)

// ImplementerService is a struct that defines what the service has
type ImplementerService struct {
	repo repository.ImplementerRepo
}

// NewImplementerService function that creates an Implementer service and returns it's reference
func NewImplementerService(repo repository.ImplementerRepo) *ImplementerService {
	return &ImplementerService{
		repo,
	}
}

// Post function that saves the Implementer to the database and logs any errors before returning a generic error
func (is *ImplementerService) Post(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	body, _ := ioutil.ReadAll(r.Body)

	if len(body) == 0 {
		log.Error("Failed to create Implementer due to missing request body")
		boom.BadData(w, "Missing request body")
		return
	}

	Implementer, err := is.repo.Insert(r.Context(), body)
	if err != nil {
		log.Error("Failed to create Implementer", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	ImplementerBytes := new(bytes.Buffer)
	if err := json.NewEncoder(ImplementerBytes).Encode(Implementer); err != nil {
		log.Error("Failed to convert orm model to bytes for Implementer", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(ImplementerBytes.Bytes()); err != nil {
		log.Error("Failed to write Implementer to response", zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

// Get function is not currently used for ImplementerService
func (is *ImplementerService) Get(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Delete function is not currently used for ImplementerService
func (is *ImplementerService) Delete(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Put function is not currently used for ImplementerService
func (is *ImplementerService) Put(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	implID, ok := r.Context().Value(middleware.ContextKeyImplementer).(string)
	if !ok {
		log.Error("Failed to extract implementer id from context")
		boom.Internal(w, "Internal error")
		return
	}

	impl, err := is.repo.FindByID(r.Context(), implID)
	if err != nil {
		log.Error("Failed to update Implementer", zap.Error(err))
		boom.Internal(w, "Internal server error")
		return
	}
	if impl == nil {
		log.Error(fmt.Sprintf("Implementer not found, id: %s", implID))
		boom.NotFound(w, "Implementer not found")
		return
	}
	body, _ := ioutil.ReadAll(r.Body)

	if len(body) == 0 {
		log.Error("Failed to update Implementer due to missing request body")
		boom.BadData(w, "Missing request body")
		return
	}

	var ImplementerModel v2.Implementer
	if err := json.Unmarshal(body, &ImplementerModel); err != nil {
		log.Error("Failed to unmarshal implementer", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if ImplementerModel.Name == "" {
		log.Error("Failed to update implementer due to missing name in body")
		boom.BadData(w, "Missing name in body")
		return
	}

	implementer, err := is.repo.Update(r.Context(), implID, body)
	if err != nil {
		log.Error("Failed to update Implementer", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	ImplementerBytes := new(bytes.Buffer)
	if err := json.NewEncoder(ImplementerBytes).Encode(implementer); err != nil {
		log.Error("Failed to convert orm model to bytes for implementer", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(ImplementerBytes.Bytes()); err != nil {
		log.Error("Failed to write implementer to response", zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

// Export function that starts an export job for a given Group ID
func (is *ImplementerService) Export(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}
