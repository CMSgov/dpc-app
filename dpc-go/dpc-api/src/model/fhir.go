package model

import "github.com/samply/golang-fhir-models/fhir-models/fhir"

// ResourceType is a reusable struct to include the resourceTypes in the below structs
type ResourceType struct {
	ResourceType string `json:"resourceType"`
}

// Organization is a struct that represents the filtered down fhir.Organization
type Organization struct {
	Identifier []fhir.Identifier `json:"identifier,omitempty"`
	Name       *string           `json:"name,omitempty"`
	ResourceType
}

// Group is a struct that represents the filtered down fhir.Group
type Group struct {
	Type           fhir.GroupType  `json:"type"`
	Actual         bool            `bson:"actual" json:"actual"`
	Name           *string         `json:"name,omitempty"`
	ManagingEntity *fhir.Reference `json:"managingEntity,omitempty"`
	Member         []GroupMember   `json:"member,omitempty"`
	ResourceType
}

// GroupMember is a struct that represent the filtered down fhir.GroupMember
type GroupMember struct {
	Entity    *fhir.Reference `json:"entity"`
	Extension []Extension     `json:"extension,omitempty"`
}

// Extension is a struct that represents the DaVinci structure definition
type Extension struct {
	URL            string          `json:"url"`
	ValueReference *fhir.Reference `json:"valueReference"`
}
