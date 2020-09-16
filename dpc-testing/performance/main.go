package main

import (
	"crypto/rsa"
	"fmt"
	"os"
	"time"

	dpcclient "github.com/CMSgov/dpc-app/dpcclient/lib"
	vegeta "github.com/tsenart/vegeta/lib"
)

const orgID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0"

var (
	apiURL, adminURL, resultsPath, pubKeyStr, signature, keyID, accessToken string
	goldenMacaroon, clientToken                                             []byte
	privateKey                                                              *rsa.PrivateKey
)

func init() {
	initFlags()

	createDirs()

	goldenMacaroon = getClientToken("")

	createOrg()

	pubKeyStr, privateKey, signature = getKeyPairAndSignature()

	keyID = uploadKey(pubKeyStr, signature)

	clientToken = getClientToken(orgID)
}

func main() {

	testMetadata()
	testKeyEndpoints()
	testTokenEndpoints()

	cleanUp()
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

func testKeyEndpoints() {
	refreshAccessToken()

	postKeyTarget := vegeta.Target{
		Method: "POST",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Key"),
		Header: map[string][]string{
			"Content-Type":  {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
		Body: []byte(fmt.Sprintf("{ \"key\": \"%s\", \"signature\": \"%s\" }", pubKeyStr, signature)),
	}

	runTest(postKeyTarget, 5, 5)

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
		URL:    fmt.Sprintf("%s%s%s", apiURL, "/Key/", keyID),
		Header: map[string][]string{
			"Accept":        {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}
	runTest(getKeyTarget, 5, 5)

	deleteKeyTarget := vegeta.Target{
		Method: "DELETE",
		URL:    fmt.Sprintf("%s%s%s", apiURL, "/Key/", keyID),
		Header: map[string][]string{
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}
	runTest(deleteKeyTarget, 5, 5)
}

func testTokenEndpoints() {
	refreshAccessToken()

	postTokenTarget := vegeta.Target{
		Method: "POST",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Token"),
		Header: map[string][]string{
			"Content-Type":  {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}

	runTest(postTokenTarget, 5, 5)

	postTokenAuthTarget := vegeta.Target{
		Method: "POST",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Token/auth"),
		Header: map[string][]string{
			"Content-Type":  {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}

	runTest(postTokenAuthTarget, 5, 5)

	getTokensTarget := vegeta.Target{
		Method: "GET",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Token"),
		Header: map[string][]string{
			"Accept":        {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}

	runTest(getTokensTarget, 5, 5)

	getTokenTarget := vegeta.Target{
		Method: "GET",
		URL:    fmt.Sprintf("%s%s%s", apiURL, "/Token/", "tokenID"),
		Header: map[string][]string{
			"Accept":        {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}

	runTest(getTokenTarget, 5, 5)

	validateTokenTarget := vegeta.Target{
		Method: "POST",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Token/validate"),
		Header: map[string][]string{
			"Content-Type":  {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}

	runTest(validateTokenTarget, 5, 5)
}

func refreshAccessToken() {
	authToken, err := dpcclient.GenerateAuthToken(privateKey, keyID, clientToken, apiURL)
	if err != nil {
		cleanAndPanic(err)
	}

	accessToken, err = dpcclient.GetAccessToken(authToken, apiURL)
	if err != nil {
		cleanAndPanic(err)
	}
}

func runTest(target vegeta.Target, duration, frequency int) {
	fmt.Printf("Running performance test on %s %s...\n", target.Method, target.URL)

	d := time.Second * time.Duration(duration)
	r := vegeta.Rate{Freq: frequency, Per: time.Second}

	attacker := vegeta.NewAttacker()
	var metrics vegeta.Metrics
	for results := range attacker.Attack(vegeta.NewStaticTargeter(target), r, d, fmt.Sprintf("%dps:", r.Freq)) {
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
