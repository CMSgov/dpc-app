package service

import (
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/darahayes/go-boom"
	"go.uber.org/zap"
	"net/http"
)

type DataService struct {
	jr repository.JobRepo
}

func NewDataService() *DataService {
	queueDbV1 := repository.GetQueueDbConnection()
	jr := repository.NewJobRepo(queueDbV1)
	return &DataService{
		jr,
	}
}

func (ds *DataService) CheckFile(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	fileName, ok := r.Context().Value(middleware.ContextKeyFileName).(string)
	if !ok {
		log.Error("Failed to extract file name from context")
		boom.BadRequest(w, "Could not get file name in URL")
		return
	}

	orgID, ok := r.Context().Value(middleware.ContextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract organization ID from context")
		boom.BadRequest(w, "Could not get organization ID")
		return
	}

	fi, err := ds.jr.IsFileValid(r.Context(), orgID, fileName)
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
