package v1

import (
	"database/sql"
	"time"

	"github.com/google/uuid"
)

// JobQueueBatch is a struct that models the v1 job_queue_batch table
type JobQueueBatch struct {
	JobID           uuid.UUID    `db:"job_id" json:"job_id"`
	OrganizationID  string       `db:"organization_id" json:"organization_id"`
	OrganizationNPI string       `db:"organization_npi" json:"organization_npi"`
	ProviderNPI     string       `db:"provider_npi" json:"provider_npi"`
	PatientMBIs     string       `db:"patients" json:"patient_mbis"`
	ResourceTypes   string       `db:"resource_types" json:"resource_types"`
	Since           sql.NullTime `db:"since" json:"since"`
	TransactionTime time.Time    `db:"transaction_time" json:"transaction_time"`
	Priority        int          `db:"priority" json:"priority"`
	Status          int          `db:"status" json:"status"`
	SubmitTime      time.Time    `db:"submit_time" json:"submit_time"`
	RequestingIP    string       `db:"requesting_ip" json:"requesting_ip"`
	IsBulk          bool         `db:"is_bulk" json:"is_bulk"`
}
