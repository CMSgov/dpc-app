package v2

import (
	"time"

	"gopkg.in/guregu/null.v3"
)

// Implementer is a struct that models the Implementer table
type Implementer struct {
	ID        string    `db:"id" json:"id" faker:"uuid_hyphenated"`
	Name      string    `db:"name" json:"name" faker:"name"`
	CreatedAt time.Time `db:"created_at" json:"created_at" faker:"-"`
	UpdatedAt time.Time `db:"updated_at" json:"updated_at" faker:"-"`
	DeletedAt null.Time `db:"deleted_at" json:"deleted_at,omitempty" faker:"-"`
}
