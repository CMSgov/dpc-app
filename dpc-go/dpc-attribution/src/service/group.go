package service

import (
	"bytes"
	"encoding/json"
	v1 "github.com/CMSgov/dpc/attribution/service/v1"
	"io/ioutil"
	"net/http"

	"github.com/darahayes/go-boom"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
)

// GroupService is a struct that defines what the service has
type GroupService struct {
	repo repository.GroupRepo
	js   v1.JobService
}

// NewGroupService function that creates a group service and returns it's reference
func NewGroupService(repo repository.GroupRepo, js v1.JobService) *GroupService {
	return &GroupService{
		repo,
		js,
	}
}

// Post function that saves the group to the database and logs any errors before returning a generic error
func (gs *GroupService) Post(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	body, _ := ioutil.ReadAll(r.Body)

	group, err := gs.repo.Insert(r.Context(), body)
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

// Get function is not currently used for v2.GroupService
func (gs *GroupService) Get(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Delete function is not currently used for v2.GroupService
func (gs *GroupService) Delete(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}

// Put function is not currently used for v2.GroupService
func (gs *GroupService) Put(w http.ResponseWriter, r *http.Request) {
	w.WriteHeader(http.StatusNotImplemented)
}
