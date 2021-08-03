package model

import (
	"database/sql"
	"time"
)

// Implementer is a struct that models the Implementer table
type Implementer struct {
	ID          string       `db:"id" json:"id" faker:"uuid_hyphenated"`
	Name        string       `db:"name" json:"name" faker:"name"`
	SsasGroupID string       `db:"ssas_group_id" json:"ssas_group_id" faker:"uuid_hyphenated"`
	CreatedAt   time.Time    `db:"created_at" json:"created_at" faker:"-"`
	UpdatedAt   time.Time    `db:"updated_at" json:"updated_at" faker:"-"`
	DeletedAt   sql.NullTime `db:"deleted_at" json:"deleted_at,omitempty" faker:"-"`
}
