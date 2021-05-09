package model

import (
    "database/sql"
    "encoding/json"
    "time"
)

// ImplementerOrgRelation is a struct that models the Implementer_org_relation table
type ImplementerOrgRelation struct {
	ID              string       `db:"id" json:"id"`
	Implementer_ID  string       `db:"implementer_id" json:"implementor_id"`
	Organization_ID string       `db:"organization_id" json:"organization_id"`
	CreatedAt       time.Time    `db:"created_at" json:"created_at"`
	UpdatedAt       time.Time    `db:"updated_at" json:"updated_at"`
	DeletedAt       sql.NullTime `db:"deleted_at" json:"deleted_at,omitempty"`
    Status        ImplOrgStatus `db:"status" json:"status,omitempty"`

}

func (u *ImplementerOrgRelation) MarshalJSON() ([]byte, error) {
    type Alias ImplementerOrgRelation
    return json.Marshal(&struct {
        Status string `json:"status"`
  //      DeletedAt string `json:"deleted_at,omitempty"`
        *Alias
    }{
        Status: u.Status.String(),
 //       DeletedAt: MarshalNullTime(u.DeletedAt),
        Alias:    (*Alias)(u),
    })
}
//
//func MarshalNullTime(t sql.NullTime) string{
//    if t.Valid {
//        b,_ := t.Time.MarshalJSON()
//        return string(b)
//    }
//    return ""
//}