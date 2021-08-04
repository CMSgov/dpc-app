package model

import (
	"github.com/pkg/errors"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
)

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

// GroupContainer is a struct that represents a minimum amount of info from attribution
type GroupContainer struct {
	ID   string `json:"id"`
	Info Group  `json:"info"`
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

// Attribution is a struct that attributes a provider with a patient
type Attribution struct {
	ProviderNPI string
	PatientMBI  string
}

// GetAttributionInfo is a func that gets the attribution relationships contained in the group
func (g *Group) GetAttributionInfo() ([]Attribution, error) {
	npis := make([]Attribution, 0)
	for _, m := range g.Member {
		prac := m.FindPractitionerRef()
		if prac == nil {
			continue
		}
		pracNPI, err := getReferenceIdentifier(prac, "http://hl7.org/fhir/sid/us-npi")
		if err != nil {
			continue
		}
		patientMBI, err := getReferenceIdentifier(m.Entity, "http://hl7.org/fhir/sid/us-mbi")
		if err != nil {
			continue
		}
		npis = append(npis, Attribution{
			ProviderNPI: pracNPI,
			PatientMBI:  patientMBI,
		})
	}
	return npis, nil
}

// FindPractitionerRef is a func that gets the practitioner reference from the group members
func (member *GroupMember) FindPractitionerRef() *fhir.Reference {
	for _, e := range member.Extension {
		vr := e.ValueReference
		if vr != nil && vr.Type != nil && *vr.Type == "Practitioner" {
			return vr
		}
	}
	return nil
}

// FindPractitionerExtension is a func that gets the practitioner extension from the group members
func (member *GroupMember) FindPractitionerExtension() *Extension {
	for _, e := range member.Extension {
		vr := e.ValueReference
		if vr != nil && vr.Type != nil && *vr.Type == "Practitioner" {
			return &e
		}
	}
	return nil
}

func getReferenceIdentifier(reference *fhir.Reference, system string) (string, error) {
	if reference == nil {
		return "", errors.New("Did not pass in a reference")
	}
	referenceSystem := reference.Identifier.System
	if referenceSystem == nil && *referenceSystem != system {
		return "", errors.New("Did not find a valid identifier")
	}
	v := reference.Identifier.Value
	if v == nil {
		return "", errors.New("Did not find a valid identifier")
	}
	return *v, nil
}
