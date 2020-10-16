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
	testMetadata()

	orgID := createOrg()
	defer func() {
		if err := recover(); err != nil {
			fmt.Printf("recovering from: %q\n", err)
		}

		cleanUp(orgID)
	}()

	pubKeyStr, privateKey, signature := generateKeyPairAndSignature()
	keyID := uploadKey(pubKeyStr, signature, orgID)
	clientToken := getClientToken(orgID)

	accessToken := refreshAccessToken(privateKey, keyID, clientToken)
	testKeyEndpoints(accessToken)

	accessToken = refreshAccessToken(privateKey, keyID, clientToken)
	testTokenEndpoints(accessToken, privateKey, keyID, clientToken)

	accessToken = refreshAccessToken(privateKey, keyID, clientToken)
	testPatientEndpoints(accessToken)

	accessToken = refreshAccessToken(privateKey, keyID, clientToken)
	testOrganizationEndpoints(accessToken)
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
