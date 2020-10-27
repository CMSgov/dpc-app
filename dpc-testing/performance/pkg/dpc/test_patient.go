package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunPatientTests() {
	const endpoint = "Patient"

	// Create organization (and delete at the end) and setup accesstoken
	auth := api.SetUpOrgAuth()
	defer api.DeleteOrg(auth.orgID)

	bundleBodies := readBodies("../../src/main/resources/parameters/bundles/patients/patient-*.json")
	bodies := readBodies("../../src/main/resources/patients/patient-*.json")

	// POST /Patient/$validate
	targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint + "/$validate",
		AccessToken: auth.accessToken,
		Bodies:      bundleBodies,
	}).Run(5, 2)

	// POST /Patient
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		Bodies:      bodies,
	}).Run(5, 2)

	// Retrieve patient IDs which are required by the remaining tests
	patientIDs := unmarshalIDs(resps)

	// POST /Patient/$submit
	targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint + "/$submit",
		AccessToken: auth.accessToken,
		Bodies:      bundleBodies,
	}).Run(5, 2)

	// PUT /Patient/{id}
	targeter.New(targeter.Config{
		Method:      "PUT",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		IDs:         patientIDs,
		Bodies:      bodies,
	}).Run(5, 2)

	// DELETE /Patient/{id}
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		IDs:         patientIDs,
	}).Run(5, 2)
}
