package model

import (
    "database/sql"
    "time"
)

// Implementer is a struct that models the Implementer table
type Implementer struct {
	ID        string    `db:"id" json:"id"`
	Name      string    `db:"name" json:"name"`
	CreatedAt time.Time `db:"created_at" json:"created_at"`
	UpdatedAt time.Time `db:"updated_at" json:"updated_at"`
	DeletedAt sql.NullTime `db:"deleted_at" json:"deleted_at,omitempty"`
}
