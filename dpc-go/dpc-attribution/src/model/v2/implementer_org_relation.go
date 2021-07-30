package v2

import (
	"database/sql"
	"encoding/json"
	"time"
)

// ImplementerOrgRelation is a struct that models the Implementer_org_relation table
type ImplementerOrgRelation struct {
	ID             string        `db:"id" json:"id" faker:"uuid_hyphenated"`
	ImplementerID  string        `db:"implementer_id" json:"implementer_id" faker:"uuid_hyphenated"`
	OrganizationID string        `db:"organization_id" json:"org_id" faker:"uuid_hyphenated"`
	CreatedAt      time.Time     `db:"created_at" json:"created_at" faker:"-"`
	UpdatedAt      time.Time     `db:"updated_at" json:"updated_at" faker:"-"`
	DeletedAt      sql.NullTime  `db:"deleted_at" json:"-" faker:"-"`
	Status         ImplOrgStatus `db:"status" json:"status,omitempty" faker:"-"`
	SsasSystemID   string        `db:"ssas_system_id" json:"ssas_system_id,omitempty" faker:"-"`
}

// ImplementorOrgOutput is a struct for output from attribution
type ImplementorOrgOutput struct {
	ImplementerOrgRelation
	NPI string `json:"npi"`
}

// MarshalJSON Json marshaller
func (u *ImplementerOrgRelation) MarshalJSON() ([]byte, error) {
	type Alias ImplementerOrgRelation
	return json.Marshal(&struct {
		Status string `json:"status"`
		*Alias
	}{
		Status: u.Status.String(),
		Alias:  (*Alias)(u),
	})
}

// MarshalJSON Json marshaller
func (u *ImplementorOrgOutput) MarshalJSON() ([]byte, error) {
	type Alias ImplementorOrgOutput
	return json.Marshal(&struct {
		Status string `json:"status"`
		*Alias
	}{
		Status: u.Status.String(),
		Alias:  (*Alias)(u),
	})
}
