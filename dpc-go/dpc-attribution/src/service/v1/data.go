package service

import (
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/repository/v1"
	"github.com/CMSgov/dpc/attribution/util"
	"github.com/darahayes/go-boom"
	"go.uber.org/zap"
)

// DataService is an interface for testing to be able to mock the services in the router test
type DataService interface {
	GetFileInfo(w http.ResponseWriter, r *http.Request)
}

// DataServiceV1 struct defines the class
type DataServiceV1 struct {
	jr v1.JobRepo
}

// NewDataService creates a dataservice
func NewDataService(jr v1.JobRepo) DataService {
	return &DataServiceV1{
		jr,
	}
}

// GetFileInfo gets the file info for a filename
func (ds *DataServiceV1) GetFileInfo(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	fileName := util.FetchValueFromContext(r.Context(), w, middleware.ContextKeyFileName)
	orgID := util.FetchValueFromContext(r.Context(), w, middleware.ContextKeyOrganization)

	fi, err := ds.jr.GetFileInfo(r.Context(), orgID, fileName)
	if err != nil {
		log.Error(fmt.Sprintf("File name: %s is not valid", fileName), zap.Error(err))
		boom.BadRequest(w, "file name doesn't check out")
		return
	}

	b, err := json.Marshal(fi)
	if err != nil {
		log.Error("FileInfo failed to marshal to bytes", zap.Error(err))
		boom.BadRequest(w, "file name doesn't check out")
		return
	}

	if _, err := w.Write(b); err != nil {
		log.Error("Failed to write file info to response", zap.Error(err))
		boom.Internal(w, err.Error())
	}
}
