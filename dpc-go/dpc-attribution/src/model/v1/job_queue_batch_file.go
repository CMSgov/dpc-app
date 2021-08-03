package v1

import (
	"database/sql/driver"
	"encoding/hex"

	"github.com/pkg/errors"
)

type resourceType string
type hexType string

// JobQueueBatchFile is a struct that models the v1 job queue batch file table
type JobQueueBatchFile struct {
	ResourceType *resourceType `db:"resource_type" json:"resourceType"`
	BatchID      string        `db:"batch_id" json:"batchID"`
	Sequence     int           `db:"sequence" json:"sequence"`
	FileName     string        `db:"file_name" json:"fileName"`
	Count        int           `db:"count" json:"count"`
	Checksum     *hexType      `db:"checksum" json:"checksum"`
	FileLength   int           `db:"file_length" json:"fileLength"`
}

// Scan is a function to convert the database int to string resource type representation
func (rt *resourceType) Scan(value interface{}) error {
	if bv, err := driver.Int32.ConvertValue(value); err == nil {
		if v, ok := bv.(int64); ok {
			*rt = resourceType(resourceMap[int(v)])
			return nil
		}
	}
	return errors.New("failed to scan resourceType")
}

// Scan is a function to convert the checksum stored in the database to hex string
func (ht *hexType) Scan(value interface{}) error {
	if v, ok := value.([]byte); ok {
		*ht = hexType(hex.EncodeToString(v))
		return nil
	}
	return nil
}

var resourceMap = map[int]string{5: "OperationOutcome", 3: "ExplanationOfBenefit", 1: "Coverage", 7: "Patient"}
