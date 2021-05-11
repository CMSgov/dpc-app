package v2

import (
	"bytes"
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/logger"
    "github.com/CMSgov/dpc/attribution/middleware"
    "github.com/CMSgov/dpc/attribution/repository"
	"github.com/darahayes/go-boom"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
)

// ImplementerService is a struct that defines what the service has
type ImplementerService struct {
	repo repository.ImplementerRepo
    orgRelRepo repository.ImplementerOrgRepo
}

// NewImplementerService function that creates an Implementer service and returns it's reference
func NewImplementerService(repo repository.ImplementerRepo, orgRelRepo repository.ImplementerOrgRepo) *ImplementerService {
	return &ImplementerService{
		repo, orgRelRepo,
	}
}

// Post function that saves the Implementer to the database and logs any errors before returning a generic error
func (os *ImplementerService) Post(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	body, _ := ioutil.ReadAll(r.Body)

	if len(body) == 0 {
		log.Error("Failed to create Implementer due to missing request body")
		boom.BadData(w, "Missing request body")
		return
	}

	Implementer, err := os.repo.Insert(r.Context(), body)
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


// Get function that get the organization from the database by id and logs any errors before returning a generic error
func (is *ImplementerService) Get(w http.ResponseWriter, r *http.Request) {
    log := logger.WithContext(r.Context())
    implementerId, ok := r.Context().Value(middleware.ContextKeyImplementer).(string)
    if !ok {
        log.Error("Failed to extract implementer id from context")
        boom.BadRequest(w, "Could not get implementer id")
        return
    }

    impl, err := is.repo.FindByID(r.Context(), implementerId)
    if err != nil {
        log.Error("Failed to retrieve implementer", zap.Error(err))
        boom.NotFound(w, err.Error())
        return
    }

    relations, err := is.orgRelRepo.FindManagedOrgs(r.Context(), implementerId)




    implBytes := new(bytes.Buffer)
    if err := json.NewEncoder(implBytes).Encode(impl); err != nil {
        log.Error("Failed to convert orm model to bytes for implementer", zap.Error(err))
        boom.Internal(w, err.Error())
        return
    }

    if _, err := w.Write(implBytes.Bytes()); err != nil {
        log.Error("Failed to write organization to response for organization", zap.Error(err))
        boom.Internal(w, err.Error())
    }
}
