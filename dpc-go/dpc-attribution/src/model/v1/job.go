package v1

import (
	"github.com/google/uuid"
)

// Job is a struct that models the v1 Rosters table
type Job struct {
	ID uuid.UUID `db:"job_id" json:"id"`
}
