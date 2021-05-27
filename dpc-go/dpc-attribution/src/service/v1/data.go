package service

import (
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/CMSgov/dpc/attribution/util"
	"github.com/darahayes/go-boom"
	"go.uber.org/zap"
	"net/http"
)

// DataService struct defines the class
type DataService struct {
	jr repository.JobRepo
}

// NewDataService creates a dataservice
func NewDataService(jr repository.JobRepo) *DataService {
	return &DataService{
		jr,
	}
}

// GetFileInfo gets the file info for a filename
func (ds *DataService) GetFileInfo(w http.ResponseWriter, r *http.Request) {
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
		log.Error(fmt.Sprintf("FileInfo failed to marshal to bytes"), zap.Error(err))
		boom.BadRequest(w, "file name doesn't check out")
		return
	}

	if _, err := w.Write(b); err != nil {
		log.Error("Failed to write file info to response", zap.Error(err))
		boom.Internal(w, err.Error())
	}
}
