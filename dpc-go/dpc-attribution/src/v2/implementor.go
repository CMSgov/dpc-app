package v2

import (
    "bytes"
    "encoding/json"
    "fmt"
    "github.com/CMSgov/dpc/attribution/logger"
    "github.com/CMSgov/dpc/attribution/repository"
    "github.com/darahayes/go-boom"
    "go.uber.org/zap"
    "io/ioutil"
    "net/http"
)

// ImplementorService is a struct that defines what the service has
type ImplementorService struct {
	repo repository.ImplementorRepo
}

// NewImplementorService function that creates an Implementor service and returns it's reference
func NewImplementorService(repo repository.ImplementorRepo) *ImplementorService {
	return &ImplementorService{
		repo,
	}
}

// Post function that saves the implementor to the database and logs any errors before returning a generic error
func (os *ImplementorService) Post(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	body, _ := ioutil.ReadAll(r.Body)

	implementor, err := os.repo.Insert(r.Context(), body)
	if err != nil {
		log.Error("Failed to create implementor", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	implementorBytes := new(bytes.Buffer)
	if err := json.NewEncoder(implementorBytes).Encode(implementor); err != nil {
		log.Error(fmt.Sprintf("Failed to convert orm model to bytes for implementor"), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(implementorBytes.Bytes()); err != nil {
		log.Error(fmt.Sprintf("Failed to write implementor to response"), zap.Error(err))
		boom.Internal(w, err.Error())
	}
}
