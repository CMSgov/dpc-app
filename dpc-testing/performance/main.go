package main

import (
	"crypto/rsa"
	"encoding/json"
	"fmt"
	"os"
	"time"

	dpcclient "github.com/CMSgov/dpc-app/dpcclient/lib"
	vegeta "github.com/tsenart/vegeta/lib"
)

var (
	apiURL, adminURL string
	goldenMacaroon   []byte
	keyIDs           []string
)

func init() {
	initFlags()
	createDirs()
	goldenMacaroon = getClientToken("")
}

func main() {
	testMetadata()

	orgID := createOrg()
	pubKeyStr, privateKey, signature := generateKeyPairAndSignature()
	keyID := uploadKey(pubKeyStr, signature, orgID)
	clientToken := getClientToken(orgID)

	accessToken := refreshAccessToken(privateKey, keyID, clientToken)
	testKeyEndpoints(accessToken)

	cleanUp(orgID)
}

func testMetadata() {
	getMetadataTarget := vegeta.Target{
		Method: "GET",
		URL:    fmt.Sprintf("%s%s", apiURL, "/metadata"),
		Header: map[string][]string{
			"Accept": {"application/fhir+json"},
		},
	}

	runTest(getMetadataTarget, 5, 5)
}

func testKeyEndpoints(accessToken string) {

	resps := runTestWithTargeter(fmt.Sprintf("POST %s/Key", apiURL), newPOSTKeyTargeter(accessToken), 5, 5)
	for _, resp := range resps {
		var result Resource
		json.Unmarshal(resp, &result)
		keyIDs = append(keyIDs, result.ID)
	}

	getKeysTarget := vegeta.Target{
		Method: "GET",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Key"),
		Header: map[string][]string{
			"Accept":        {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}
	runTest(getKeysTarget, 5, 5)

	getKeyTarget := vegeta.Target{
		Method: "GET",
		URL:    fmt.Sprintf("%s%s%s", apiURL, "/Key/", keyIDs[0]),
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

func refreshAccessToken(privateKey *rsa.PrivateKey, keyID string, clientToken []byte) string {
	authToken, err := dpcclient.GenerateAuthToken(privateKey, keyID, clientToken, apiURL)
	if err != nil {
		cleanAndPanic(err)
	}

	accessToken, err := dpcclient.GetAccessToken(authToken, apiURL)
	if err != nil {
		cleanAndPanic(err)
	}

	return accessToken
}

func runTest(target vegeta.Target, duration, frequency int) [][]byte {
	return runTestWithTargeter(fmt.Sprintf("%s %s", target.Method, target.URL), vegeta.NewStaticTargeter(target), duration, frequency)
}

func runTestWithTargeter(name string, targeter vegeta.Targeter, duration, frequency int) [][]byte {

	fmt.Printf("\nRunning performance test on %s...\n", name)

	d := time.Second * time.Duration(duration)
	r := vegeta.Rate{Freq: frequency, Per: time.Second}

	attacker := vegeta.NewAttacker()
	var metrics vegeta.Metrics
	var respBodies [][]byte
	for results := range attacker.Attack(targeter, r, d, fmt.Sprintf("%dps:", r.Freq)) {
		metrics.Add(results)
		respBodies = append(respBodies, results.Body)
	}
	metrics.Close()

	reporter := vegeta.NewJSONReporter(&metrics)
	reporter.Report(os.Stdout)

	return respBodies
}

func cleanAndPanic(err error) {
	cleanUp()
	panic(err)
}
