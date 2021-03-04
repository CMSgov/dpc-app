package middleware

import (
	"context"
	"encoding/json"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
)

type resourceType struct {
	ResourceType string `json:"resourceType"`
}

type organization struct {
	Identifier   []fhir.Identifier      `json:"identifier"`
	Name         string                 `json:"name"`
	Address      []fhir.Address         `json:"address"`
	Type         []fhir.CodeableConcept `json:"type"`
	ResourceType string                 `json:"resourceType"`
}

var filters = map[string]func([]byte) ([]byte, error){
	"Organization": filterOrganization,
}

// Filter is a function that filters out all FHIR fields that aren't explicitly whitelisted
func Filter(ctx context.Context, body []byte) ([]byte, error) {
	log := logger.WithContext(ctx)
	rt, err := getResourceType(body)
	if err != nil {
		return nil, err
	}
	log.Info("resource type: " + rt)
	return filters[rt](body)
}

func getResourceType(body []byte) (string, error) {
	var result resourceType
	if err := json.Unmarshal(body, &result); err != nil {
		return "", err
	}
	return result.ResourceType, nil
}

func filterOrganization(body []byte) ([]byte, error) {
	var organization organization
	if err := json.Unmarshal(body, &organization); err != nil {
		return nil, err
	}
	return json.Marshal(organization)
}
