package main

import (
	"bufio"
	"bytes"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"flag"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strings"
	"time"

	vegeta "github.com/tsenart/vegeta/lib"
	"github.com/tsenart/vegeta/lib/plot"

	dpcclient "github.com/CMSgov/dpc-app/dpcclient/lib"
)

const orgID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0"

var (
	apiURL, adminURL, resultsPath, requestMethod, endpoint string
	freq, duration                                         int
	goldenMacaroon                                         []byte
)

func init() {
	initFlags()

	createDirs()

	goldenMacaroon = getClientToken("")

	createOrg()

	pubKeyStr, privKey, signature := getKeyPairAndSignature()

	keyID := uploadKey(pubKeyStr, signature)

	clientToken := getClientToken(orgID)

	authToken, err := dpcclient.GenerateAuthToken(privKey, keyID, clientToken, apiURL)
	if err != nil {
		panic(err)
	}
	fmt.Println("Auth token = " + string(authToken))
}

func initFlags() {
	flag.StringVar(&apiURL, "api_url", "http://localhost:3002/v1", "Base URL of API")
	flag.StringVar(&adminURL, "admin_url", "http://localhost:9903/tasks", "Base URL of admin tasks")
	flag.StringVar(&resultsPath, "result_path", "results", "Path where results files will be written")
	flag.StringVar(&requestMethod, "request_method", "GET", "HTTP request method to test")
	flag.StringVar(&endpoint, "endpoint", "/metadata", "Endpoint to test")
	flag.IntVar(&duration, "duration", 5, "Total time to run the test, in seconds")
	flag.Parse()
}

func createDirs() {
	err := os.MkdirAll(resultsPath, os.ModePerm)
	if err != nil {
		panic(err)
	}

	err = os.MkdirAll("keys", os.ModePerm)
	if err != nil {
		panic(err)
	}

	err = os.MkdirAll("tokens", os.ModePerm)
	if err != nil {
		panic(err)
	}
}

func getClientToken(orgID string) []byte {
	reqURL := fmt.Sprintf("%s/generate-token", adminURL)
	if orgID != "" {
		reqURL = fmt.Sprintf("%s?organization=%s", reqURL, orgID)
	}
	resp, err := http.Post(reqURL, "", nil)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()
	clientToken, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		panic(err)
	}
	return clientToken
}

func createOrg() {
	orgBundleFile, _ := os.Open("../../src/main/resources/organization_bundle_parameters.json")
	defer orgBundleFile.Close()
	orgBundleReader := bufio.NewReader(orgBundleFile)
	req, err := http.NewRequest("POST", fmt.Sprintf("%s/Organization/$submit", apiURL), orgBundleReader)
	req.Header.Add("Content-Type", "application/fhir+json")
	req.Header.Add("Authorization", fmt.Sprintf("Bearer %s", goldenMacaroon))
	_, err = http.DefaultClient.Do(req)
	if err != nil {
		panic(err)
	}
}

func getKeyPairAndSignature() (string, *rsa.PrivateKey, string) {
	privKey, pubKey, err := dpcclient.GenRSAKeyPair()
	if err := dpcclient.SaveDPCKeyPair("./keys/dpc-key", privKey, pubKey); err != nil {
		panic(err)
	}

	pubKeyBytes, err := dpcclient.ReadSmallFile("./keys/dpc-key-public.pem")
	pubKeyStr := strings.ReplaceAll(string(pubKeyBytes), "\n", "\\n")

	snippet := []byte("This is the snippet used to verify a key pair in DPC.")
	snippetHash := sha256.New()
	_, err = snippetHash.Write(snippet)
	if err != nil {
		panic(err)
	}

	signature, err := privKey.Sign(rand.Reader, snippetHash.Sum(nil), crypto.SHA256)
	if err != nil {
		panic(err)
	}
	sigEnc := base64.StdEncoding.EncodeToString(signature)

	return pubKeyStr, privKey, sigEnc
}

func uploadKey(key, sig string) string {
	type Key struct {
		ID string `json:"id"`
	}
	keySigReader := strings.NewReader(fmt.Sprintf("{ \"key\": \"%s\", \"signature\": \"%s\" }", key, sig))
	req, err := http.NewRequest("POST", fmt.Sprintf("%s/upload-key?organization=%s", adminURL, "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0"), keySigReader)
	req.Header.Add("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()
	keyResp, _ := ioutil.ReadAll(resp.Body)
	var result Key
	json.Unmarshal(keyResp, &result)
	return result.ID
}

func main() {
	// targeter := makeTarget()
	// apiResults := runTest(targeter)

	// var buf bytes.Buffer
	// _, err := apiResults.WriteTo(&buf)
	// if err != nil {
	// 	panic(err)
	// }

	//	writeResults(fmt.Sprintf("%s_%s_plot", requestMethod, strings.ReplaceAll(endpoint, "/", "")), buf)

	cleanUp()
}

func makeTarget() vegeta.Targeter {
	url := fmt.Sprintf("%s%s", apiURL, endpoint)

	accessToken := ""

	header := map[string][]string{
		"Prefer":        {"respond-async"},
		"Accept":        {"application/fhir+json"},
		"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
	}

	return vegeta.NewStaticTargeter(vegeta.Target{
		Method: "GET",
		URL:    url,
		Header: header,
	})
}

func runTest(target vegeta.Targeter) *plot.Plot {
	fmt.Printf("Running performance test on %s %s...\n", requestMethod, endpoint)
	title := plot.Title(fmt.Sprintf("test_%s_%s", requestMethod, endpoint))
	p := plot.New(title)
	defer p.Close()

	d := time.Second * time.Duration(duration)
	rate := vegeta.Rate{Freq: freq, Per: time.Second}
	plotAttack(p, target, rate, d)

	return p
}

func plotAttack(p *plot.Plot, t vegeta.Targeter, r vegeta.Rate, du time.Duration) {
	attacker := vegeta.NewAttacker()
	var metrics vegeta.Metrics
	for results := range attacker.Attack(t, r, du, fmt.Sprintf("%dps:", r.Freq)) {
		metrics.Add(results)
		err := p.Add(results)
		if err != nil {
			panic(err)
		}
	}
	metrics.Close()

	fmt.Printf("99th percentile: %s\n", metrics.Latencies.P99)
}

func writeResults(filename string, buf bytes.Buffer) {
	data := buf.Bytes()
	if len(data) > 0 {
		fn := fmt.Sprintf("%s/%s.html", resultsPath, filename)
		fmt.Printf("Writing results: %s\n", fn)
		err := ioutil.WriteFile(fn, data, 0600)
		if err != nil {
			panic(err)
		}
	}
}

func cleanUp() {
	deleteOrg()
	deleteDirs()
}

func deleteOrg() {
	req, err := http.NewRequest("DELETE", fmt.Sprintf("%s/Organization/%s", apiURL, orgID), nil)
	req.Header.Add("Authorization", fmt.Sprintf("Bearer %s", goldenMacaroon))
	if err != nil {
		panic(err)
	}

}

func deleteDirs() {
	err := os.RemoveAll(resultsPath)
	if err != nil {
		panic(err)
	}

	err = os.RemoveAll("keys")
	if err != nil {
		panic(err)
	}

	err = os.RemoveAll("tokens")
	if err != nil {
		panic(err)
	}
}
