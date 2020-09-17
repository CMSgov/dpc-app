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
	orgID := createOrg()

	// pubKeyStr, privateKey, signature := getKeyPairAndSignature()
	// keyID := uploadKey(pubKeyStr, signature, orgID)
	// clientToken := getClientToken(orgID)
	// accessToken := refreshAccessToken(privateKey, keyID, clientToken)

	testMetadata()

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
