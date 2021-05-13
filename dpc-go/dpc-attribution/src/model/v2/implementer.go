package v2

import (
	"time"

	"gopkg.in/guregu/null.v3"
)

// Implementer is a struct that models the Implementer table
type Implementer struct {
	ID        string    `db:"id" json:"id"`
	Name      string    `db:"name" json:"name"`
	CreatedAt time.Time `db:"created_at" json:"created_at"`
	UpdatedAt time.Time `db:"updated_at" json:"updated_at"`
	DeletedAt null.Time `db:"deleted_at" json:"deleted_at,omitempty"`
}
