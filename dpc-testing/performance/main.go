package main

import (
	"crypto/rsa"
	"fmt"
	"os"
	"time"

	dpcclient "github.com/CMSgov/dpc-app/dpcclient/lib"
	vegeta "github.com/tsenart/vegeta/lib"
)

var (
	apiURL, adminURL string
	goldenMacaroon   []byte
)

func init() {
	initFlags()
	createDirs()
	goldenMacaroon = getClientToken("")
}

func main() {
	//testMetadata()

	orgID := createOrg()
	pubKeyStr, privateKey, signature := getKeyPairAndSignature()
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

	runTestWithTargeter(newPOSTKeyTargeter(accessToken), 5, 5)

	// getKeysTarget := vegeta.Target{
	// 	Method: "GET",
	// 	URL:    fmt.Sprintf("%s%s", apiURL, "/Key"),
	// 	Header: map[string][]string{
	// 		"Accept":        {"application/json"},
	// 		"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
	// 	},
	// }
	// runTest(getKeysTarget, 5, 5)

	// getKeyTarget := vegeta.Target{
	// 	Method: "GET",
	// 	URL:    fmt.Sprintf("%s%s%s", apiURL, "/Key/", keyID),
	// 	Header: map[string][]string{
	// 		"Accept":        {"application/json"},
	// 		"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
	// 	},
	// }
	// runTest(getKeyTarget, 5, 5)

	// deleteKeyTarget := vegeta.Target{
	// 	Method: "DELETE",
	// 	URL:    fmt.Sprintf("%s%s%s", apiURL, "/Key/", keyID),
	// 	Header: map[string][]string{
	// 		"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
	// 	},
	// }
	// runTest(deleteKeyTarget, 5, 5)
}

func newPOSTKeyTargeter(accessToken string) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "POST"
		t.URL = fmt.Sprintf("%s%s", apiURL, "/Key")
		t.Header = map[string][]string{
			"Content-Type":  {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		}

		pubKeyStr, _, signature := getKeyPairAndSignature()
		body := fmt.Sprintf("{ \"key\": \"%s\", \"signature\": \"%s\"}", pubKeyStr, signature)
		t.Body = []byte(body)

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

func runTest(target vegeta.Target, duration, frequency int) {
	fmt.Printf("Running performance test on %s %s...\n", target.Method, target.URL)
	runTestWithTargeter(vegeta.NewStaticTargeter(target), duration, frequency)
}

func runTestWithTargeter(targeter vegeta.Targeter, duration, frequency int) {
	fmt.Printf("Running performance test on %s...\n", targeter)

	d := time.Second * time.Duration(duration)
	r := vegeta.Rate{Freq: frequency, Per: time.Second}

	attacker := vegeta.NewAttacker()
	var metrics vegeta.Metrics
	for results := range attacker.Attack(targeter, r, d, fmt.Sprintf("%dps:", r.Freq)) {
		metrics.Add(results)
	}
	metrics.Close()

	reporter := vegeta.NewJSONReporter(&metrics)
	reporter.Report(os.Stdout)
}

func cleanAndPanic(err error) {
	cleanUp()
	panic(err)
}
