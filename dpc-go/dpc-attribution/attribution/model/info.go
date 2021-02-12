package model

import (
	"database/sql/driver"
	"encoding/json"
	"github.com/pkg/errors"
)

type Info map[string]interface{}

func (p Info) Value() (driver.Value, error) {
	j, err := json.Marshal(p)
	return j, err
}

func (p *Info) Scan(src interface{}) error {
	source, ok := src.([]byte)
	if !ok {
		return errors.New("Type assertion .([]byte) failed.")
	}

	var i interface{}
	err := json.Unmarshal(source, &i)
	if err != nil {
		return err
	}

	*p, ok = i.(map[string]interface{})
	if !ok {
		return errors.New("Type assertion .(map[string]interface{}) failed.")
	}

	return nil
}
