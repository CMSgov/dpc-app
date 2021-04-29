package v1

import (
	uuid "github.com/jackc/pgx/pgtype/ext/gofrs-uuid"
)

// Job is a struct that models the v1 Rosters table
type Job struct {
	ID             uuid.UUID `db:"id" json:"id"`
}
