package model

import (
	"database/sql/driver"
	"encoding/json"
	"github.com/pkg/errors"
)

type Info map[string]interface{}

func (i Info) Value() (driver.Value, error) {
	info, err := json.Marshal(i)
	return info, err
}

func (i *Info) Scan(src interface{}) error {
	source, ok := src.([]byte)
	if !ok {
		return errors.New("Type assertion .([]byte) failed.")
	}

	var info interface{}
	err := json.Unmarshal(source, &info)
	if err != nil {
		return err
	}

	*i, ok = info.(map[string]interface{})
	if !ok {
		return errors.New("Type assertion .(map[string]interface{}) failed.")
	}

	return nil
}
