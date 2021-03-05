package middleware

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/pkg/errors"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"strings"
)

type resourceType struct {
	ResourceType string `json:"resourceType"`
}

type organization struct {
	Identifier []fhir.Identifier      `json:"identifier"`
	Name       string                 `json:"name"`
	Address    []fhir.Address         `json:"address"`
	Type       []fhir.CodeableConcept `json:"type"`
	resourceType
}

type patient struct {
	Identifier []fhir.Identifier         `json:"identifier"`
	Name       []fhir.HumanName          `json:"name"`
	Gender     fhir.AdministrativeGender `json:"gender"`
	BirthDate  string                    `json:"birthDate"`
	resourceType
}

type practitioner struct {
	Identifier []fhir.Identifier `json:"identifier"`
	Name       []fhir.HumanName  `json:"name"`
	resourceType
}

var filters = map[string]func([]byte) ([]byte, error){
	"organization": filterOrganization,
	"patient":      filterPatient,
	"practitioner": filterPractitioner,
}

// Filter is a function that filters out all FHIR fields that aren't explicitly whitelisted
func Filter(ctx context.Context, body []byte) ([]byte, error) {
	log := logger.WithContext(ctx)
	rt, err := getResourceType(body)
	if err != nil {
		return nil, err
	}
	log.Debug(fmt.Sprintf("Filtering out data for resource type %s", rt))
	fn := filters[rt]
	if fn == nil {
		return nil, errors.New("Resource type not found to filter")
	}
	return filters[rt](body)
}

func getResourceType(body []byte) (string, error) {
	var result resourceType
	if err := json.Unmarshal(body, &result); err != nil {
		return "", err
	}
	return strings.ToLower(result.ResourceType), nil
}

func filterOrganization(body []byte) ([]byte, error) {
	var organization organization
	if err := json.Unmarshal(body, &organization); err != nil {
		return nil, err
	}
	return json.Marshal(organization)
}

func filterPatient(body []byte) ([]byte, error) {
	var patient patient
	if err := json.Unmarshal(body, &patient); err != nil {
		return nil, err
	}
	return json.Marshal(patient)
}

func filterPractitioner(body []byte) ([]byte, error) {
	var practitioner practitioner
	if err := json.Unmarshal(body, &practitioner); err != nil {
		return nil, err
	}
	return json.Marshal(practitioner)
}
