package model

import (
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/util"
	"time"
)

// Group is a struct that models the group table
type Group struct {
	ID             string    `db:"id" json:"id" faker:"uuid_hyphenated"`
	Version        int       `db:"version" json:"version" faker:"-"`
	CreatedAt      time.Time `db:"created_at" json:"created_at" faker:"-"`
	UpdatedAt      time.Time `db:"updated_at" json:"updated_at" faker:"-"`
	Info           Info      `db:"info" json:"info" faker:"-"`
	OrganizationID string    `db:"organization_id" json:"organizationId" faker:"uuid_hyphenated"`
}

/**
*Majority of this stuff below can go away after shared job service
**/

// Attribution is a struct that attributes a provider with a patient
type Attribution struct {
	ProviderNPI string
	PatientMBI  string
}

// MemberContainer is a struct that has members
type MemberContainer struct {
	Members []GroupMember `json:"member"`
}

// GroupMember is a struct that represent the filtered down fhir.GroupMember
type GroupMember struct {
	Entity    *Reference  `json:"entity"`
	Extension []Extension `json:"extension,omitempty"`
}

// Extension is a struct that represents the DaVinci structure definition
type Extension struct {
	URL            string     `json:"url"`
	ValueReference *Reference `json:"valueReference"`
}

// Reference is a struct that represents a Reference
type Reference struct {
	Type       string      `json:"type"`
	Identifier interface{} `json:"identifier"`
}

// GetAttributionInfo is a func that gets the attribution relationships contained in the group
func (g Group) GetAttributionInfo() ([]Attribution, error) {
	i := g.Info
	b, err := json.Marshal(i)
	if err != nil {
		return nil, err
	}

	var grp MemberContainer
	if err := json.Unmarshal(b, &grp); err != nil {
		return nil, err
	}

	npis := make([]Attribution, 0)
	for _, m := range grp.Members {
		prac := findPractitionerRef(m)
		if prac == nil {
			continue
		}
		pracNPI, err := util.GetReferenceNPI(prac)
		if err != nil {
			continue
		}
		patientMBI, err := util.GetReferenceMBI(m.Entity)
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

func findPractitionerRef(member GroupMember) *Reference {
	for _, m := range member.Extension {
		vr := m.ValueReference
		if vr != nil && vr.Type == "Practitioner" {
			return vr
		}
	}
	return nil
}
