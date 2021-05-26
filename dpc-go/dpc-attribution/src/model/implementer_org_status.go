package model

import "database/sql/driver"

// ImplOrgStatus represents the implementer org relationship status
type ImplOrgStatus int64

// Scan value deserializer
func (u *ImplOrgStatus) Scan(value interface{}) error { *u = ImplOrgStatus(value.(int64)); return nil }

// Value db value converter
func (u ImplOrgStatus) Value() (driver.Value, error) { return int64(u), nil }

const (
	// Unknown Zero value
	Unknown ImplOrgStatus = iota

	// Pending relation not yet active
	Pending

	// Active Relation is active
	Active
)

func (u ImplOrgStatus) String() string {
	return [...]string{"Unknown", "Pending", "Active"}[u]
}
