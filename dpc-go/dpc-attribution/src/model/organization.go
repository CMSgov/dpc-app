package model

import (
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/util"
	"time"
)

// Organization is a struct that models the organization table
type Organization struct {
	ID        string    `db:"id" json:"id" faker:"uuid_hyphenated"`
	Version   int       `db:"version" json:"version" faker:"-"`
	CreatedAt time.Time `db:"created_at" json:"created_at" faker:"-"`
	UpdatedAt time.Time `db:"updated_at" json:"updated_at" faker:"-"`
	Info      Info      `db:"info" json:"info" faker:"-"`
}

/**
* this stuff below can go away after shared job service
**/

// GetNPI returns the NPI of the org
func (o Organization) GetNPI() (string, error) {
	b, err := json.Marshal(o.Info)
	if err != nil {
		return "", err
	}
	return util.GetNPI(b)
}
