package model

import (
	uuid "github.com/jackc/pgx/pgtype/ext/gofrs-uuid"
)

// Group is a struct that models the v1 Rosters table
type Group struct {
	ID             uuid.UUID `db:"id" json:"id"`
	ProviderID     string    `db:"provider_id" json:"provider_id"`
	OrganizationId uuid.UUID `db:"organization_id" json:"organization_id"`
}
