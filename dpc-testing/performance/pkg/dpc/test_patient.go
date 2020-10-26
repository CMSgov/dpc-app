package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunPatientTests(accessToken string) {
	const ENDPOINT = "Patient"

	// POST /Patient/$validate
	targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT + "/$validate",
		AccessToken: accessToken,
		Bodies:      readBodies("../../src/main/resources/parameters/bundles/patients/patient-*.json"),
	}).Run(5, 2)

	// POST /Patient
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessToken,
		Bodies:      readBodies("../../src/main/resources/patients/patient-*.json"),
	}).Run(5, 2)

	// Retrieve patient IDs which are required by the remaining tests
	patientIDs := unmarshalIDs(resps)

	// POST /Patient/$submit
	targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT + "/$submit",
		AccessToken: accessToken,
		Bodies:      readBodies("../../src/main/resources/parameters/bundles/patients/patient-*.json"),
	}).Run(5, 2)

	// PUT /Patient/{id}
	targeter.New(targeter.Config{
		Method:      "PUT",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessToken,
		IDs:         patientIDs,
		Bodies:      readBodies("../../src/main/resources/patients/patient-*.json"),
	}).Run(5, 2)

	// DELETE /Patient/{id}
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessToken,
		IDs:         patientIDs,
	}).Run(5, 2)
}
