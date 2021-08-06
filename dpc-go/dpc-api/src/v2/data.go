package v2

import (
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/constants"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/model"
	"go.uber.org/zap"
	"net/http"
	"path/filepath"
)

// DataController is a struct that defines what the controller has
type DataController struct {
	c client.DataClient
}

// NewDataController function that creates a data controller and returns it's reference
func NewDataController(c client.DataClient) *DataController {
	return &DataController{
		c,
	}
}

// GetFile function that calls attribution service via get to check valid file in attribution service and then returns the file
func (dc *DataController) GetFile(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())

	fileName, ok := r.Context().Value(constants.ContextKeyFileName).(string)
	if !ok {
		log.Error("Failed to extract file name from context")
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	b, err := dc.c.Data(r.Context(), fmt.Sprintf("validityCheck/%s", fileName[:len(fileName)-len(filepath.Ext(fileName))]))
	if err != nil {
		log.Error(fmt.Sprintf("Failed to check if file %s is valid", fileName), zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusNotFound, fmt.Sprintf("Failed to get file %s", fileName))
		return
	}

	var fileInfo model.FileInfo
	err = json.Unmarshal(b, &fileInfo)
	if err != nil {
		log.Error("Failed to unmarshal json to map", zap.Error(err))
		fhirror.GenericServerIssue(r.Context(), w)
	}

	w.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=\"%s.ndjson\"", fileInfo.FileName))
	w.Header().Set("Content-Type", "application/octet-stream")

	exportPath := conf.GetAsString("exportPath")
	http.ServeFile(w, r, fmt.Sprintf("%s/%s.ndjson", exportPath, fileInfo.FileName))
}
