package dpc

import (
	"fmt"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunGroupTests() {
	const endpoint = "Group"

	// Create organization (and delete at the end) and setup accesstoken
	auth := api.SetUpOrgAuth()
	defer api.DeleteOrg(auth.orgID)

	pracBodies := readBodies("../../src/main/resources/practitioners/practitioner-*.json")
	grpBodies := readBodies("../../src/main/resources/groups/group-*.json")

	// POST /Practitioner
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    "Practitioner",
		AccessToken: auth.accessToken,
		Bodies:      pracBodies,
	}).Run(5, 2)

	pracIDs := unmarshalIDs(resps)

	var xProvValues []string
	for _, id := range pracIDs {
		xProvValues = append(xProvValues, fmt.Sprintf("{ \"resourceType\": \"Provenance\", \"meta\": { \"profile\": [ \"https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-attestation\" ] }, \"recorded\": \"1990-01-01T00:00:00.000-05:00\", \"reason\": [ { \"system\": \"http://hl7.org/fhir/v3/ActReason\", \"code\": \"TREAT\" } ], \"agent\": [ { \"role\": [ { \"coding\": [ { \"system\": \"http://hl7.org/fhir/v3/RoleClass\", \"code\": \"AGNT\" } ] } ], \"whoReference\": { \"reference\": \"Organization/%s}}\" }, \"onBehalfOfReference\": { \"reference\": \"Practitioner/%s\" } } ] }", auth.orgID, id))
	}

	// POST /Group
	resps = targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		Headers: &targeter.Headers{
			ContentType: FHIR,
			Accept:      FHIR,
			Custom: map[string][]string{
				"X-Provenance": xProvValues,
			},
		},
		Bodies: grpBodies,
	}).Run(5, 2)

	// Retrieve group IDs which are required by the remaining tests
	grpIDs := unmarshalIDs(resps)

	// GET /Group
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
	}).Run(5, 2)

	// GET /Group/{id}
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		IDs:         grpIDs,
		AccessToken: auth.accessToken,
	}).Run(5, 2)

	// PUT /Group/{id}
	targeter.New(targeter.Config{
		Method:   "PUT",
		BaseURL:  api.URL,
		Endpoint: endpoint,
		Headers: &targeter.Headers{
			ContentType: FHIR,
			Accept:      FHIR,
			Custom: map[string][]string{
				"X-Provenance": xProvValues,
			},
		},
		Bodies:      grpBodies,
		IDs:         grpIDs,
		AccessToken: auth.accessToken,
	}).Run(5, 2)

	// DELETE /Group/{id}
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		IDs:         grpIDs,
	}).Run(5, 2)
}
