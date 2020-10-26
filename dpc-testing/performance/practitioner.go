package main

import (
	"container/list"
	"encoding/json"
	"fmt"

	vegeta "github.com/tsenart/vegeta/lib"
)

func testPractitionerEndpoints(accessToken string) {
	// POST /Practitioner/$validate
	postPractitionerValidateTargeter := newPOSTPractitionerTargeter("/$validate", accessToken, nextPractParams, 1)
	// Higher numbers of requests cause timeouts
	runTestWithTargeter(fmt.Sprintf("POST %s/Practitioner/$validate", apiURL), postPractitionerValidateTargeter, 2, 2)

	// POST /Practitioner
	postPractitionerTargeter := newPOSTPractitionerTargeter("", accessToken, nextPractitioner, 1)
	resps := runTestWithTargeter(fmt.Sprintf("POST %s/Practitioner", apiURL), postPractitionerTargeter, 2, 2)

	var practitionerIDs = list.New()
	for _, resp := range resps {
		var result Resource
		json.Unmarshal(resp, &result)
		practitionerIDs.PushBack(result.ID)
	}

	// POST /Practitioner/$submit
	postPractitionerSubmitTargeter := newPOSTPractitionerTargeter("/$submit", accessToken, nextPractParams, 1)
	// Higher numbers of requests cause timeouts
	resps = runTestWithTargeter(fmt.Sprintf("POST %s/Practitioner/$submit", apiURL), postPractitionerSubmitTargeter, 2, 2)

	// PUT /Practitioner/{id}
	currentPractitionerID := practitionerIDs.Front()
	putPractitionerTargeter := newPUTPractitioner(func() string {
		s, ok := currentPractitionerID.Value.(string)
		if !ok {
			cleanAndPanic(fmt.Errorf("not a valid string: %v", currentPractitionerID.Value))
		}
		currentPractitionerID = currentPractitionerID.Next()
		return s
	}, accessToken, nextPractitioner, 1)
	runTestWithTargeter(fmt.Sprintf("PUT %s/Practitioner/{id}", apiURL), putPractitionerTargeter, 2, 2)

	// DELETE /Practitioner/{id}
	currentPractitionerID = practitionerIDs.Front()
	deletePractitionerTarget := newDELETEPractitionerTargeter(func() string {
		id := currentPractitionerID.Value.(string)
		currentPractitionerID = currentPractitionerID.Next()
		return id
	}, accessToken)
	runTestWithTargeter(fmt.Sprintf("DELETE %s/Practitioner/{id}", apiURL), deletePractitionerTarget, 2, 2)
}

func nextPractParams(fileNum *int) []byte {
	return nextFile("../../src/main/resources/parameters/bundles/practitioners/practitioner-%v.json", fileNum)
}

func nextPractitioner(fileNum *int) []byte {
	return nextFile("../../src/main/resources/practitioners/practitioner-%v.json", fileNum)
}

func newPOSTPractitionerTargeter(operation, accessToken string, nextBody func(*int) []byte, fileNum int) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "POST"
		t.URL = fmt.Sprintf("%s/Practitioner%s", apiURL, operation)
		t.Header = map[string][]string{
			"Content-Type":  {"application/fhir+json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		}
		t.Body = nextBody(&fileNum)

		return nil
	}
}

func newPUTPractitioner(nextPractitionerID func() string, accessToken string, nextBody func(*int) []byte, fileNum int) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "PUT"
		t.URL = fmt.Sprintf("%s/Practitioner/%s", apiURL, nextPractitionerID())
		t.Header = map[string][]string{
			"Content-Type":  {"application/fhir+json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		}
		t.Body = nextBody(&fileNum)

		return nil
	}
}

func newDELETEPractitionerTargeter(nextPractitionerID func() string, accessToken string) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "DELETE"
		t.URL = fmt.Sprintf("%s/Practitioner/%s", apiURL, nextPractitionerID())
		t.Header = map[string][]string{
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		}

		return nil
	}
}