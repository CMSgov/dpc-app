package main

import (
	"fmt"

	vegeta "github.com/tsenart/vegeta/lib"
)

func testPractionerEndpoints(accessToken string) {
	// POST /Practitioner/$validate
	postPractionerValidateTargeter := newPOSTPractionerTargeter("/$validate", accessToken, nextParameters, 1)
	// Higher numbers of requests cause timeouts
	runTestWithTargeter(fmt.Sprintf("POST %s/Patient/$validate", apiURL), postPractionerValidateTargeter, 5, 2)
}

func newPOSTPractionerTargeter(operation, accessToken string, nextBody func(*int) []byte, fileNum int) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "POST"
		t.URL = fmt.Sprintf("%s/Practioner%s", apiURL, operation)
		t.Header = map[string][]string{
			"Content-Type":  {"application/fhirs+json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
			"Accept":        {"application/fhir+json"},
		}
		t.Body = nextBody(&fileNum)

		return nil
	}
}