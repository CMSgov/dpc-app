package main

import (
	"bufio"
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

	dpcclient "github.com/CMSgov/dpc-app/dpcclient/lib"
)

type Resource struct {
	ID string `json:"id"`
}

func initFlags() {
	flag.StringVar(&apiURL, "api_url", "http://localhost:3002/v1", "Base URL of API")
	flag.StringVar(&adminURL, "admin_url", "http://localhost:9903/tasks", "Base URL of admin tasks")
	flag.Parse()
}

func createDirs() {
	err := os.MkdirAll("keys", os.ModePerm)
	if err != nil {
		cleanAndPanic(err)
	}

	err = os.MkdirAll("tokens", os.ModePerm)
	if err != nil {
		cleanAndPanic(err)
	}
}

func getClientToken(orgID string) []byte {
	reqURL := fmt.Sprintf("%s/generate-token", adminURL)
	if orgID != "" {
		reqURL = fmt.Sprintf("%s?organization=%s", reqURL, orgID)
	}
	resp, err := http.Post(reqURL, "", nil)
	if err != nil {
		cleanAndPanic(err)
	}
	defer resp.Body.Close()
	clientToken, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		cleanAndPanic(err)
	}
	return clientToken
}

func createOrg() string {
	orgBundleFile, _ := os.Open("../../src/main/resources/organization_bundle_parameters.json")
	defer orgBundleFile.Close()
	orgBundleReader := bufio.NewReader(orgBundleFile)
	req, err := http.NewRequest("POST", fmt.Sprintf("%s/Organization/$submit", apiURL), orgBundleReader)
	req.Header.Add("Content-Type", "application/fhir+json")
	req.Header.Add("Authorization", fmt.Sprintf("Bearer %s", goldenMacaroon))
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		cleanAndPanic(err)
	}
	defer resp.Body.Close()
	orgResp, _ := ioutil.ReadAll(resp.Body)
	var result Resource
	json.Unmarshal(orgResp, &result)
	return result.ID
}

func getKeyPairAndSignature() (string, *rsa.PrivateKey, string) {
	privKey, pubKey, err := dpcclient.GenRSAKeyPair()
	if err := dpcclient.SaveDPCKeyPair("./keys/dpc-key", privKey, pubKey); err != nil {
		cleanAndPanic(err)
	}

	pubKeyBytes, err := dpcclient.ReadSmallFile("./keys/dpc-key-public.pem")
	pubKeyStr := strings.ReplaceAll(string(pubKeyBytes), "\n", "\\n")

	snippet := []byte("This is the snippet used to verify a key pair in DPC.")
	snippetHash := sha256.New()
	_, err = snippetHash.Write(snippet)
	if err != nil {
		cleanAndPanic(err)
	}

	signature, err := privKey.Sign(rand.Reader, snippetHash.Sum(nil), crypto.SHA256)
	if err != nil {
		cleanAndPanic(err)
	}
	sigEnc := base64.StdEncoding.EncodeToString(signature)

	return pubKeyStr, privKey, sigEnc
}

func uploadKey(key, sig, orgID string) string {
	keySigReader := strings.NewReader(fmt.Sprintf("{ \"key\": \"%s\", \"signature\": \"%s\" }", key, sig))
	req, err := http.NewRequest("POST", fmt.Sprintf("%s/upload-key?organization=%s", adminURL, orgID), keySigReader)
	if err != nil {
		cleanAndPanic(err)
	}
	req.Header.Add("Content-Type", "application/json")
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		cleanAndPanic(err)
	}
	defer resp.Body.Close()
	keyResp, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		cleanAndPanic(err)
	}
	var result Resource
	json.Unmarshal(keyResp, &result)
	return result.ID
}

func cleanUp(orgIDs ...string) {
	for _, orgID := range orgIDs {
		deleteOrg(orgID)
	}
	deleteDirs()
}

func deleteOrg(orgID string) {
	req, err := http.NewRequest("DELETE", fmt.Sprintf("%s/Organization/%s", apiURL, orgID), nil)
	if err != nil {
		fmt.Println("Organization could not be deleted", err)
	}
	req.Header.Add("Authorization", fmt.Sprintf("Bearer %s", goldenMacaroon))
	_, err = http.DefaultClient.Do(req)
	if err != nil {
		fmt.Println("Organization could not be deleted", err)
	}

}

func deleteDirs() {
	err := os.RemoveAll("keys")
	if err != nil {
		fmt.Println("keys directory could not be deleted", err)
	}

	err = os.RemoveAll("tokens")
	if err != nil {
		fmt.Println("tokens directory could not be deleted", err)
	}
}
