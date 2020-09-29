package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"

	vegeta "github.com/tsenart/vegeta/lib"
)

func testPatientEndpoints(accessToken string) {
	parametersJSON, err := ioutil.ReadFile("../../src/main/resources/patient_parameters_bundle-1.json")
	if err != nil {
		cleanAndPanic(err)
	}

	// POST /Patient/$validate
	postPatientValidateTarget := vegeta.Target{
		Method: "POST",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Patient/$validate"),
		Header: map[string][]string{
			"Content-Type":  {"application/fhir+json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		},
		Body: parametersJSON,
	}
	// Higher numbers of requests cause timeouts
	runTest(postPatientValidateTarget, 5, 2)

	patientJSON, err := ioutil.ReadFile("../../src/main/resources/patient.json")
	if err != nil {
		cleanAndPanic(err)
	}

	// POST /Patient
	postPatientTarget := vegeta.Target{
		Method: "POST",
		URL:    fmt.Sprintf("%s/Patient", apiURL),
		Header: map[string][]string{
			"Content-Type":  {"application/fhir+json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		},
		Body: patientJSON,
	}
	resps := runTest(postPatientTarget, 5, 5)

	var patientIDs []string
	for _, resp := range resps {
		var result Resource
		json.Unmarshal(resp, &result)
		patientIDs = append(patientIDs, result.ID)
	}

	// POST /Patient/$submit
	postPatientSubmitTarget := vegeta.Target{
		Method: "POST",
		URL:    fmt.Sprintf("%s/Patient/$submit", apiURL),
		Header: map[string][]string{
			"Content-Type":  {"application/fhir+json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		},
		Body: parametersJSON,
	}
	runTest(postPatientSubmitTarget, 5, 2)

	// PUT /Patient/{id}
	putPatientTarget := vegeta.Target{
		Method: "PUT",
		URL:    fmt.Sprintf("%s/Patient/%s", apiURL, patientIDs[0]),
		Header: map[string][]string{
			"Content-Type":  {"application/fhir+json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		},
		Body: patientJSON,
	}
	runTest(putPatientTarget, 5, 5)

	// DELETE /Patient/{id}
	deletePatientTarget := vegeta.Target{
		Method: "DELETE",
		URL:    fmt.Sprintf("%s/Patient/%s", apiURL, patientIDs[0]),
		Header: map[string][]string{
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}
	runTest(deletePatientTarget, 5, 5)
}
