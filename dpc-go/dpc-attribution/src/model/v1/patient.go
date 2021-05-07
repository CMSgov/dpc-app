package v1

import (
	"github.com/google/uuid"
)

// Patient is a struct that models the v1 Patients table
type Patient struct {
	ID  uuid.UUID `db:"id" json:"id"`
	MBI string    `db:"beneficiary_id" json:"mbi"`
}
