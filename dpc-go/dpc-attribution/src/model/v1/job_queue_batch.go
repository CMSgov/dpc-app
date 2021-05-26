package v1

import (
	"database/sql"
	"database/sql/driver"
	"github.com/pkg/errors"
	"strings"
	"time"
)

var statusMap = map[int]string{0: "QUEUED", 1: "RUNNING", 2: "COMPLETED", 3: "FAILED"}

type statusType string

// JobQueueBatch is a struct that models the v1 job_queue_batch table
type JobQueueBatch struct {
	BatchID         string        `db:"batch_id" json:"batchId" faker:"uuid_hyphenated"`
	PatientMBIs     string        `db:"patients" json:"patient_mbis" faker:"uuid_digit"`
	TransactionTime time.Time     `db:"transaction_time" json:"transaction_time" faker:"-"`
	Status          statusType    `db:"status" json:"status" faker:"-"`
	SubmitTime      time.Time     `db:"submit_time" json:"submit_time" faker:"-"`
	RequestURL      string        `db:"request_url" json:"request_url" faker:"url"`
	PatientIndex    sql.NullInt64 `db:"patient_index"`
	CompleteTime    sql.NullTime  `db:"complete_time" faker:"-"`
}

func (st *statusType) Scan(value interface{}) error {
	if bv, err := driver.Int32.ConvertValue(value); err == nil {
		if v, ok := bv.(int64); ok {
			*st = statusType(statusMap[int(v)])
			return nil
		}
	}
	return errors.New("failed to scan resourceType")
}

func (jqb *JobQueueBatch) PatientsProcessed() int {
	results := 0
	if jqb.Status == "COMPLETED" {
		if jqb.PatientMBIs != "" {
			results = len(jqb.Patients())
		}
	} else {
		results = 0
		if jqb.PatientIndex.Valid {
			results = int(jqb.PatientIndex.Int64) + 1
		}
	}
	return results
}

func (jqb *JobQueueBatch) Patients() []string {
	return strings.Split(jqb.PatientMBIs, ",")
}
