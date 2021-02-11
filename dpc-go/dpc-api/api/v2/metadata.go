package v2

import (
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
	"os"
	"time"

	"github.com/samply/golang-fhir-models/fhir-models/fhir"
)

type MetadataController struct {
	capabilitiesFile string
}

func NewMetadataController(capabilitiesFile string) *MetadataController {
	return &MetadataController{
		capabilitiesFile,
	}
}

func (mc *MetadataController) Read(w http.ResponseWriter, r *http.Request) {
	const dateFormat = "2006-01-02"
	dt := time.Now()
	log := logger.WithContext(r.Context())

	releaseDate, found := os.LookupEnv("RELEASE_DATE")
	if !found {
		releaseDate = dt.Format(dateFormat)
	}

	version := os.Getenv("VERSION")

	b, err := ioutil.ReadFile(mc.capabilitiesFile)
	if err != nil {
		log.Error("Failed to read capabilities file", zap.Error(err))
		fhirror.ServerIssue(w, r.Context(), http.StatusInternalServerError, "Failed to get capabilites")
		return
	}

	statement, err := fhir.UnmarshalCapabilityStatement(b)
	if err != nil {
		log.Error("Failed to convert json to fhir capabilities statement", zap.Error(err))
		fhirror.ServerIssue(w, r.Context(), http.StatusInternalServerError, "Failed to get capabilites")
		return
	}

	statement.Date = dt.Format(dateFormat)
	statement.Software.Version = getStringPtr(version)
	statement.Software.ReleaseDate = getStringPtr(releaseDate)

	b, err = statement.MarshalJSON()
	if err != nil {
		fhirror.ServerIssue(w, r.Context(), http.StatusInternalServerError, "Failed to get capabilites")
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if _, err = w.Write(b); err != nil {
		log.Error("Failed to write data", zap.Error(err))
		fhirror.ServerIssue(w, r.Context(), http.StatusInternalServerError, "Failed to get capabilites")
	}
}

func getStringPtr(value string) *string {
	return &value
}
