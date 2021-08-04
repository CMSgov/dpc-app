package model

import (
	"database/sql/driver"
	"encoding/json"

	"github.com/pkg/errors"
)

// Info is a type used to model the jsonb column in the organization table
type Info map[string]interface{}

// Value function used by the db to convert the Info map into jsonb
func (i Info) Value() (driver.Value, error) {
	info, err := json.Marshal(i)
	return info, err
}

// Scan function used by the db Scan to convert the db row into Info type
func (i *Info) Scan(src interface{}) error {
	source, ok := src.([]byte)
	if !ok {
		return errors.New("type assertion .([]byte) failed")
	}

	var info interface{}
	err := json.Unmarshal(source, &info)
	if err != nil {
		return err
	}

	*i, ok = info.(map[string]interface{})
	if !ok {
		return errors.New("type assertion .(map[string]interface{}) failed")
	}

	return nil
}
