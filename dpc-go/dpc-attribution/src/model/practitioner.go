package model

import (
	"time"
)

// Practitioner is a struct that models the practitioner table
type Practitioner struct {
	ID        string    `db:"id" json:"id"`
	Version   int       `db:"version" json:"version"`
	CreatedAt time.Time `db:"created_at" json:"created_at"`
	UpdatedAt time.Time `db:"updated_at" json:"updated_at"`
	Info      Info      `db:"info" json:"info"`
}
