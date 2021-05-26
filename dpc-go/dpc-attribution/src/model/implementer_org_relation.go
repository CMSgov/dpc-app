package model

import (
	"database/sql"
	"encoding/json"
	"time"
)

// ImplementerOrgRelation is a struct that models the Implementer_org_relation table
type ImplementerOrgRelation struct {
	ID             string        `db:"id" json:"id"`
	ImplementerID  string        `db:"implementer_id" json:"implementer_id"`
	OrganizationID string        `db:"organization_id" json:"organization_id"`
	CreatedAt      time.Time     `db:"created_at" json:"created_at"`
	UpdatedAt      time.Time     `db:"updated_at" json:"updated_at"`
	DeletedAt      sql.NullTime  `db:"deleted_at" json:"-"`
	Status         ImplOrgStatus `db:"status" json:"status,omitempty"`
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
