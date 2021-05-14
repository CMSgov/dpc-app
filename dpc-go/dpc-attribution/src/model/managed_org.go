package model

// ManagedOrg is a struct that models the relationship status of an org and implementer
type ManagedOrg struct {
	OrganizationID string        `db:"id" json:"org_id"`
	Name           string        `db:"name" json:"org_name"`
	Status         string `db:"created_at" json:"status"`
	NPI            string    `db:"updated_at" json:"npi"`
}
