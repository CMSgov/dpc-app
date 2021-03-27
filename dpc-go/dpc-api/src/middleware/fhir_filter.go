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
	Identifier []fhir.Identifier      `json:"identifier,omitempty"`
	Name       *string                `json:"name,omitempty"`
	Address    []fhir.Address         `json:"address,omitempty"`
	Type       []fhir.CodeableConcept `json:"type,omitempty"`
	resourceType
}

type Group struct {
	Type           fhir.GroupType             `json:"type"`
	Actual         bool                       `bson:"actual" json:"actual"`
	Name           *string                    `json:"name,omitempty"`
	ManagingEntity *fhir.Reference            `json:"managingEntity,omitempty"`
	Characteristic []fhir.GroupCharacteristic `json:"characteristic,omitempty"`
	Member         []GroupMember              `json:"member,omitempty"`
	resourceType
}

type GroupMember struct {
	Entity    *fhir.Reference `json:"entity"`
	Period    *fhir.Period    `json:"period,omitempty"`
	Inactive  *bool           `json:"inactive,omitempty"`
	Extension []extension     `json:"extension,omitempty"`
}

type extension struct {
	Url            string          `json:"url"`
	ValueReference *fhir.Reference `json:"valueReference"`
}

var filters = map[string]func([]byte) ([]byte, error){
	"organization": filterOrganization,
	"group":        filterGroup,
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

func filterGroup(body []byte) ([]byte, error) {
	var group Group
	if err := json.Unmarshal(body, &group); err != nil {
		return nil, err
	}
	return json.Marshal(group)
}
