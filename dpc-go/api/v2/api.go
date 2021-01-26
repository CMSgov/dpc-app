package v2

import (
	"encoding/json"
	"net/http"
	"os"
	"time"

	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	log "github.com/sirupsen/logrus"
)

func Metadata(w http.ResponseWriter, r *http.Request) {
	const dateFormat = "2006-01-02"
	dt := time.Now()

	releaseDate, available := os.LookupEnv("RELEASE_DATE")
	if !available {
		releaseDate = dt.Format(dateFormat)
	}

	version := os.Getenv("VERSION")

	statement := fhir.CapabilityStatement{
		Status:       fhir.PublicationStatusActive,
		Date:         dt.Format(dateFormat),
		Publisher:    getStringPtr("Centers for Medicare and Medicaid Services"),
		Kind:         fhir.CapabilityStatementKindInstance,
		Instantiates: []string{"http://hl7.org/fhir/uv/bulkdata/CapabilityStatement/bulk-data"},
		Software: &fhir.CapabilityStatementSoftware{
			Name:        "Data @ Point of Care API",
			Version:     getStringPtr(version),
			ReleaseDate: getStringPtr(releaseDate),
		},
		FhirVersion: fhir.FHIRVersion4_0_1,
		Format:      []string{"application/json", "application/fhir+json"},
		Rest:        []fhir.CapabilityStatementRest{},
	}

	b, err := statement.MarshalJSON()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	var obj map[string]interface{}
	if err = json.Unmarshal(b, &obj); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if _, err = w.Write(b); err != nil {
		log.Errorf("Failed to write data %s", err.Error())
	}
}

func getStringPtr(value string) *string {
	return &value
}
