package util

import "github.com/CMSgov/dpc/attribution/pkg/model"

func GetNPI(identifiers []model.Identifier) string {
	return GetIdentifier(identifiers, "http://hl7.org/fhir/sid/us-npi")
}

func GetIdentifier(identifiers []model.Identifier, system string) string {
	vsf := make([]model.Identifier, 0)
	for _, v := range identifiers {
		if v.System == system {
			vsf = append(vsf, v)
		}
	}
	return vsf[0].Value
}
