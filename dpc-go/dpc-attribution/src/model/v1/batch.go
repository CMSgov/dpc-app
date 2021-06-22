package v1

import (
	"database/sql"
	"time"
)

//TODO: This stuff is probably applicable to both v1 and v2, in future move to appropriate package

// BatchRequest is a struct to hold details for job batches
type BatchRequest struct {
	Priority        int
	Since           *sql.NullTime
	RequestURL      string
	RequestingIP    string
	OrganizationNPI string
	ProviderNPI     string
	PatientMBIs     string
	IsBulk          bool
	ResourceTypes   string
	TransactionTime time.Time
}

// BatchAndFiles is a struct to hold batch and file info from running job
type BatchAndFiles struct {
	Batch *BatchInfo          `json:"batch"`
	Files []JobQueueBatchFile `json:"files"`
}

// BatchInfo is a struct to hold batch information
type BatchInfo struct {
	TotalPatients     int        `json:"totalPatients"`
	PatientsProcessed int        `json:"patientsProcessed"`
	PatientIndex      *int       `json:"patientIndex"`
	Status            string     `json:"status"`
	TransactionTime   time.Time  `json:"transactionTime"`
	SubmitTime        time.Time  `json:"submitTime"`
	CompleteTime      *time.Time `json:"completeTime"`
	RequestURL        string     `json:"requestURL"`
}

// NewBatchInfo is a function to construct a BatchInfo from JobQueueBatch
func NewBatchInfo(batch JobQueueBatch) *BatchInfo {
	patientIndex := -1
	if batch.PatientIndex.Valid {
		patientIndex = int(batch.PatientIndex.Int64)
	}
	return &BatchInfo{
		TotalPatients:     len(batch.PatientMBIs),
		PatientsProcessed: batch.PatientsProcessed(),
		PatientIndex:      &patientIndex,
		Status:            string(batch.Status),
		TransactionTime:   batch.TransactionTime,
		SubmitTime:        batch.SubmitTime,
		CompleteTime:      &batch.CompleteTime.Time,
		RequestURL:        batch.RequestURL,
	}
}
