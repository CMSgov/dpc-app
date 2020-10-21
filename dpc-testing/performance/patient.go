package main

import (
	"container/list"
	"encoding/json"
	"fmt"
	"io/ioutil"

	vegeta "github.com/tsenart/vegeta/lib"
)

func testPatientEndpoints(accessToken string) {
	// POST /Patient/$validate
	postPatientValidateTargeter := newPOSTPatientTargeter("/$validate", accessToken, nextParameters, 1)
	// Higher numbers of requests cause timeouts
	runTestWithTargeter(fmt.Sprintf("POST %s/Patient/$validate", apiURL), postPatientValidateTargeter, 5, 2)

	// POST /Patient
	postPatientTargeter := newPOSTPatientTargeter("", accessToken, nextPatient, 1)
	resps := runTestWithTargeter(fmt.Sprintf("POST %s/Patient", apiURL), postPatientTargeter, 5, 2)

	var patientIDs = list.New()
	for _, resp := range resps {
		var result Resource
		json.Unmarshal(resp, &result)
		patientIDs.PushBack(result.ID)
	}

	// POST /Patient/$submit
	postPatientSubmitTargeter := newPOSTPatientTargeter("/$submit", accessToken, nextParameters, 1)
	// Higher numbers of requests cause timeouts
	resps = runTestWithTargeter(fmt.Sprintf("POST %s/Patient/$submit", apiURL), postPatientSubmitTargeter, 5, 2)

	// GET /Patient/{id}
	currentPatientID := patientIDs.Front()
	getPatientTargeter := newGETPatientTargeter(func() string {
		s, ok := currentPatientID.Value.(string)
		if !ok {
			cleanAndPanic(fmt.Errorf("not a valid string: %v", currentPatientID.Value))
		}
		currentPatientID = currentPatientID.Next()
		return s
	}, accessToken)
	runTestWithTargeter(fmt.Sprintf("GET %s/Patient/{id}", apiURL), getPatientTargeter, 5, 2)

	// PUT /Patient/{id}
	currentPatientID = patientIDs.Front()
	putPatientTargeter := newPUTPatientTargeter(func() string {
		s, ok := currentPatientID.Value.(string)
		if !ok {
			cleanAndPanic(fmt.Errorf("not a valid string: %v", currentPatientID.Value))
		}
		currentPatientID = currentPatientID.Next()
		return s
	}, accessToken, nextPatient, 1)
	runTestWithTargeter(fmt.Sprintf("PUT %s/Patient/{id}", apiURL), putPatientTargeter, 5, 2)

	// DELETE /Patient/{id}
	currentPatientID = patientIDs.Front()
	deletePatientTarget := newDELETEPatientTargeter(func() string {
		id := currentPatientID.Value.(string)
		currentPatientID = currentPatientID.Next()
		return id
	}, accessToken)
	runTestWithTargeter(fmt.Sprintf("DELETE %s/Patient/{id}", apiURL), deletePatientTarget, 5, 2)
}

func nextParameters(fileNum *int) []byte {
	return nextFile("../../src/main/resources/parameters/bundles/patients/patient-%v.json", fileNum)
}

func nextPatient(fileNum *int) []byte {
	return nextFile("../../src/main/resources/patients/patient-%v.json", fileNum)
}

func nextFile(path string, fileNum *int) []byte {
	b, err := ioutil.ReadFile(fmt.Sprintf(path, *fileNum))
	if err != nil {
		cleanAndPanic(err)
	}

	(*fileNum)++

	return b
}

func newPOSTPatientTargeter(operation, accessToken string, nextBody func(*int) []byte, fileNum int) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "POST"
		t.URL = fmt.Sprintf("%s/Patient%s", apiURL, operation)
		t.Header = map[string][]string{
			"Content-Type":  {"application/fhir+json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		}
		t.Body = nextBody(&fileNum)

		return nil
	}
}

func newGETPatientTargeter(nextPatientID func() string, accessToken string) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "GET"
		t.URL = fmt.Sprintf("%s/Patient/%s", apiURL, nextPatientID())
		t.Header = map[string][]string{
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		}

		return nil
	}
}

func newPUTPatientTargeter(nextPatientID func() string, accessToken string, nextBody func(*int) []byte, fileNum int) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "PUT"
		t.URL = fmt.Sprintf("%s/Patient/%s", apiURL, nextPatientID())
		t.Header = map[string][]string{
			"Content-Type":  {"application/fhir+json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		}
		t.Body = nextBody(&fileNum)

		return nil
	}
}

func newDELETEPatientTargeter(nextPatientID func() string, accessToken string) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "DELETE"
		t.URL = fmt.Sprintf("%s/Patient/%s", apiURL, nextPatientID())
		t.Header = map[string][]string{
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		}

		return nil
	}
}
