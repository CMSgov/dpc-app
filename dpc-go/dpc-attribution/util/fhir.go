package util

import (
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/pkg/errors"
)

func GetNPI(fhirModel []byte) (string, error) {
	return GetIdentifier(fhirModel, "http://hl7.org/fhir/sid/us-npi")
}

func GetIdentifier(fhirModel []byte, system string) (string, error) {
	var r model.IdentifierContainer
	err := json.Unmarshal(fhirModel, &r)
	if err != nil {
		return "", err
	}
	vsf := make([]model.Identifier, 0)
	for _, v := range r.Identifier {
		if v.System == system {
			vsf = append(vsf, v)
		}
	}
	if len(vsf) == 0 {
		return "", errors.New("No identifiers")
	}
	return vsf[0].Value, nil
}
