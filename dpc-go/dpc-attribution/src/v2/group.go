package v2

import (
	"bytes"
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/darahayes/go-boom"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
)

// GroupService is a struct that defines what the service has
type GroupService struct {
	repo repository.GroupRepo
}

// NewGroupService function that creates a group service and returns it's reference
func NewGroupService(repo repository.GroupRepo) *GroupService {
	return &GroupService{
		repo,
	}
}

// Post function that saves the group to the database and logs any errors before returning a generic error
func (os *GroupService) Post(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	body, _ := ioutil.ReadAll(r.Body)

	group, err := os.repo.Insert(r.Context(), body)
	if err != nil {
		log.Error("Failed to create group", zap.Error(err))
		boom.BadData(w, err)
		return
	}

	groupBytes := new(bytes.Buffer)
	if err := json.NewEncoder(groupBytes).Encode(group); err != nil {
		log.Error("Failed to convert orm model to bytes for group", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	if _, err := w.Write(groupBytes.Bytes()); err != nil {
		log.Error("Failed to write group to response", zap.Error(err))
		boom.Internal(w, err.Error())
	}
}
