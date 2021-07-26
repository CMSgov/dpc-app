package v2

import (
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
	"time"

	"github.com/samply/golang-fhir-models/fhir-models/fhir"
)

// MetadataController is a struct that defines what the controller has
type MetadataController struct {
	capabilitiesFile string
}

// NewMetadataController function that creates a metadata controller and returns it's reference
func NewMetadataController(capabilitiesFile string) *MetadataController {
	return &MetadataController{
		capabilitiesFile,
	}
}

// Read function to read the capability statement from metadata controller
func (mc *MetadataController) Read(w http.ResponseWriter, r *http.Request) {
	const dateFormat = "2006-01-02"
	dt := time.Now()
	log := logger.WithContext(r.Context())

	releaseDate := conf.GetAsString("capabilities.release-date", dt.Format(dateFormat))
	version := conf.GetAsString("capabilities.version")

	b, err := ioutil.ReadFile(mc.capabilitiesFile)
	if err != nil {
		log.Error("Failed to read capabilities file", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to get capabilities")
		return
	}

	statement, err := fhir.UnmarshalCapabilityStatement(b)
	if err != nil {
		log.Error("Failed to convert json to fhir capabilities statement", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to get capabilities")
		return
	}

	statement.Date = dt.Format(dateFormat)
	statement.Software.Version = getStringPtr(version)
	statement.Software.ReleaseDate = getStringPtr(releaseDate)

	b, err = statement.MarshalJSON()
	if err != nil {
        log.Error("Failed to JSON marshal fhir capabilities statement", zap.Error(err))
        fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to get capabilities")
		return
	}

	if _, err = w.Write(b); err != nil {
		log.Error("Failed to write data", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to get capabilities")
	}
}

func getStringPtr(value string) *string {
	return &value
}
