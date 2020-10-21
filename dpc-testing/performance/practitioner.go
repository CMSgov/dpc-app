package main

import (
	"fmt"

	vegeta "github.com/tsenart/vegeta/lib"
)

func testPractitionerEndpoints(accessToken string) {
	// POST /Practitioner/$validate
	postPractitionerValidateTargeter := newPOSTPractitionerTargeter("/$validate", accessToken, nextPartParams, 1)
	// Higher numbers of requests cause timeouts
	runTestWithTargeter(fmt.Sprintf("POST %s/Practitioner/$validate", apiURL), postPractitionerValidateTargeter, 5, 2)
}

func nextPartParams(fileNum *int) []byte {
	return nextFile("../../src/main/resources/parameters/bundles/practitioners/practitioner-%v.json", fileNum)
}

func newPOSTPractitionerTargeter(operation, accessToken string, nextBody func(*int) []byte, fileNum int) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "POST"
		t.URL = fmt.Sprintf("%s/Practitioner%s", apiURL, operation)
		t.Header = map[string][]string{
			"Content-Type":  {"application/fhirs+json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		}
		t.Body = nextBody(&fileNum)

		return nil
	}
}