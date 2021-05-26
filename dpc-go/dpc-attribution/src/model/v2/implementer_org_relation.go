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
	OrganizationID string        `db:"organization_id" json:"organization_id" faker:"uuid_hyphenated"`
	CreatedAt      time.Time     `db:"created_at" json:"created_at" faker:"-"`
	UpdatedAt      time.Time     `db:"updated_at" json:"updated_at" faker:"-"`
	DeletedAt      sql.NullTime  `db:"deleted_at" json:"-" faker:"-"`
	Status         ImplOrgStatus `db:"status" json:"status,omitempty" faker:"-"`
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
