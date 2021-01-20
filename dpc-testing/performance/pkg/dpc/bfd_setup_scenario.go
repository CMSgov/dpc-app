package dpc

import (
	"fmt"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunTestSetup(orgID string, numOfPatients int) {

	// Create organization (and delete at the end) and setup accesstoken
	createdOrgID := api.CreateOrg(orgID)
	auth := api.SetUpOrgAuth(createdOrgID)

	// Create Practitioner
	resps, _ := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    "Practitioner",
		AccessToken: auth.accessToken,
		Generator:   templateBodyGenerator("./templates/practitioner-template.json", map[string]func() string{"{NPI}": generateNPI}),
	}).Run(1, 1)

	pracIDs := unmarshalIDs(resps)

	var xProvValues []string
	for _, id := range pracIDs {
		xProvValues = append(xProvValues, fmt.Sprintf("{ \"resourceType\": \"Provenance\", \"meta\": { \"profile\": [ \"https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-attestation\" ] }, \"recorded\": \"1990-01-01T00:00:00.000-05:00\", \"reason\": [ { \"system\": \"http://hl7.org/fhir/v3/ActReason\", \"code\": \"TREAT\" } ], \"agent\": [ { \"role\": [ { \"coding\": [ { \"system\": \"http://hl7.org/fhir/v3/RoleClass\", \"code\": \"AGNT\" } ] } ], \"whoReference\": { \"reference\": \"Organization/%s}}\" }, \"onBehalfOfReference\": { \"reference\": \"Practitioner/%s\" } } ] }", auth.orgID, id))
	}

	npis := unmarshalIdentifiers(resps, "http://hl7.org/fhir/sid/us-npi")

	// Create patients
	resps, _ = targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    "Patient",
		AccessToken: auth.accessToken,
		Generator:   templateBodyGenerator("./templates/patient-template.json", map[string]func() string{"{MBI}": generateMBIFromFile("./data/mbis.csv")}),
	}).Run(1, numOfPatients)

	// Retrieve patient IDs which are required by the remaining tests
	patientIDs := unmarshalIDs(resps)

	// Create Group with Patient References
	targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    "Group",
		AccessToken: auth.accessToken,
		Headers: &targeter.Headers{
			ContentType: FHIR,
			Accept:      FHIR,
			Custom: map[string][]string{
				"X-Provenance": xProvValues,
			},
		},
		Generator: templateBodyGenerator("./templates/group-with-patients-template.json", map[string]func() string{"{NPI}": targeter.GenStrs(npis), "{patients}": generatePatientEntity(patientIDs)}),
	}).Run(1, 1)

}
