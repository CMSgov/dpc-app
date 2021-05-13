package model

import "database/sql/driver"

type ImplOrgStatus int64

func (u *ImplOrgStatus) Scan(value interface{}) error { *u = ImplOrgStatus(value.(int64)); return nil }
func (u ImplOrgStatus) Value() (driver.Value, error)  { return int64(u), nil }

const (
	Unknown ImplOrgStatus = iota
	Pending
	Active
)

func (d ImplOrgStatus) String() string {
	return [...]string{"Unknown", "Pending", "Active"}[d]
}
