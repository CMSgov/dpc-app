package model

import (
	"time"
)

// Group is a struct that models the group table
type Group struct {
	ID             string    `db:"id" json:"id" faker:"uuid_hyphenated"`
	Version        int       `db:"version" json:"version" faker:"-"`
	CreatedAt      time.Time `db:"created_at" json:"created_at" faker:"-"`
	UpdatedAt      time.Time `db:"updated_at" json:"updated_at" faker:"-"`
	Info           Info      `db:"info" json:"info" faker:"-"`
	OrganizationID string    `db:"organization_id" json:"organizationId" faker:"uuid_hyphenated"`
}
