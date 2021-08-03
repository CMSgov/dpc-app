package v1

import (
	"database/sql/driver"
	"encoding/hex"
	"github.com/pkg/errors"
)

// ResourceType represents a resource type
type ResourceType string

// HexType represents the hex checksum
type HexType string

// JobQueueBatchFile is a struct that models the v1 job queue batch file table
type JobQueueBatchFile struct {
	ResourceType *ResourceType `db:"resource_type" json:"resourceType"`
	BatchID      string        `db:"batch_id" json:"batchID"`
	Sequence     int           `db:"sequence" json:"sequence"`
	FileName     string        `db:"file_name" json:"fileName"`
	Count        int           `db:"count" json:"count"`
	Checksum     *HexType      `db:"checksum" json:"checksum"`
	FileLength   int           `db:"file_length" json:"fileLength"`
}

// Scan is a function to convert the database int to string resource type representation
func (rt *ResourceType) Scan(value interface{}) error {
	if bv, err := driver.Int32.ConvertValue(value); err == nil {
		if v, ok := bv.(int64); ok {
			*rt = ResourceType(resourceMap[int(v)])
			return nil
		}
	}
	return errors.New("failed to scan resourceType")
}

// Scan is a function to convert the checksum stored in the database to hex string
func (ht *HexType) Scan(value interface{}) error {
	if v, ok := value.([]byte); ok {
		*ht = HexType(hex.EncodeToString(v))
		return nil
	}
	return nil
}

var resourceMap = map[int]string{77: "OperationOutcome", 46: "ExplanationOfBenefit", 27: "Coverage", 80: "Patient"}
