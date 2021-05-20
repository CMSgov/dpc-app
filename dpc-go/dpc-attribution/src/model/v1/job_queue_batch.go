package v1

import (
	"database/sql"
	"time"
)

// JobQueueBatch is a struct that models the v1 job_queue_batch table
type JobQueueBatch struct {
	OrganizationID  string       `db:"organization_id" json:"organization_id" faker:"uuid_hyphenated"`
	OrganizationNPI string       `db:"organization_npi" json:"organization_npi" faker:"uuid_digit"`
	ProviderNPI     string       `db:"provider_npi" json:"provider_npi" faker:"uuid_digit"`
	PatientMBIs     string       `db:"patients" json:"patient_mbis" faker:"uuid_digit"`
	ResourceTypes   string       `db:"resource_types" json:"resource_types" faker:"word"`
	Since           sql.NullTime `db:"since" json:"since" faker:"-"`
	TransactionTime time.Time    `db:"transaction_time" json:"transaction_time" faker:"-"`
	Priority        int          `db:"priority" json:"priority" faker:"oneof: 1000, 5000"`
	Status          int          `db:"status" json:"status" faker:"-"`
	SubmitTime      time.Time    `db:"submit_time" json:"submit_time" faker:"-"`
	RequestingIP    string       `db:"requesting_ip" json:"requesting_ip" faker:"ipv4"`
	RequestURL      string       `db:"request_url" json:"request_url" faker:"url"`
	IsBulk          bool         `db:"is_bulk" json:"is_bulk" faker:"-"`
}
