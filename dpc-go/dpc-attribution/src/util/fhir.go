package util

import (
	"encoding/json"
	"github.com/pkg/errors"
)

type identifier struct {
	Value  string `json:"value"`
	System string `json:"system"`
}

// IdentifierContainer is a struct that holds identifiers
// This is only used for the purpose of unmarshalling
type identifiersContainer struct {
	Identifier []identifier `json:"identifier"`
}

// GetNPI function that returns the identifier value associated with the npi system
func GetNPI(fhirModel []byte) (string, error) {
	return GetIdentifier(fhirModel, "http://hl7.org/fhir/sid/us-npi")
}

// GetIdentifier function that returns the identifier value associated with the system
func GetIdentifier(fhirModel []byte, system string) (string, error) {
	var r identifiersContainer
	err := json.Unmarshal(fhirModel, &r)
	if err != nil {
		return "", err
	}
	vsf := make([]identifier, 0)
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
