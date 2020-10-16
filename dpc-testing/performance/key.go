package main

import (
	"encoding/json"
	"fmt"

	vegeta "github.com/tsenart/vegeta/lib"
)

func testKeyEndpoints(accessToken string) {

	resps := runTestWithTargeter(fmt.Sprintf("POST %s/Key", apiURL), newPOSTKeyTargeter(accessToken), 5, 5)
	var keyIDs []string
	for _, resp := range resps {
		var result Resource
		json.Unmarshal(resp, &result)
		keyIDs = append(keyIDs, result.ID)
	}

	getKeysTarget := vegeta.Target{
		Method: "GET",
		URL:    fmt.Sprintf("%s/Key", apiURL),
		Header: map[string][]string{
			"Accept":        {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}
	runTest(getKeysTarget, 5, 5)

	getKeyTarget := vegeta.Target{
		Method: "GET",
		URL:    fmt.Sprintf("%s/Key/%s", apiURL, keyIDs[0]),
		Header: map[string][]string{
			"Accept":        {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}
	runTest(getKeyTarget, 5, 5)

	deleteKeyTargeter := newDELETEKeyTargeter(accessToken, func() string {
		keyID := keyIDs[0]
		keyIDs = keyIDs[1:]
		return keyID
	})
	runTestWithTargeter(fmt.Sprintf("DELETE %s/Key/{id}", apiURL), deleteKeyTargeter, 5, 5)
}

func newPOSTKeyTargeter(accessToken string) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "POST"
		t.URL = fmt.Sprintf("%s/Key", apiURL)
		t.Header = map[string][]string{
			"Content-Type":  {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		}

		pubKeyStr, _, signature := generateKeyPairAndSignature()
		body := fmt.Sprintf("{ \"key\": \"%s\", \"signature\": \"%s\"}", pubKeyStr, signature)
		t.Body = []byte(body)

		return nil
	}
}

func newDELETEKeyTargeter(accessToken string, nextKeyID func() string) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "DELETE"
		t.URL = fmt.Sprintf("%s/Key/%s", apiURL, nextKeyID())
		t.Header = map[string][]string{
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		}
		return nil
	}
}
