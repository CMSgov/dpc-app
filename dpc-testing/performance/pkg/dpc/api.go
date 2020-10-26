package dpc

import (
	"bufio"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strings"

	dpcclient "github.com/CMSgov/dpc-app/dpcclient/lib"
)

type API struct {
	URL, AdminURL  string
	goldenMacaroon []byte
}

func (api *API) RefreshAccessToken(privateKey *rsa.PrivateKey, keyID string, clientToken []byte) string {
	authToken, err := dpcclient.GenerateAuthToken(privateKey, keyID, clientToken, api.URL)
	if err != nil {
		cleanAndPanic(err)
	}

	accessToken, err := dpcclient.GetAccessToken(authToken, api.URL)
	if err != nil {
		cleanAndPanic(err)
	}

	return accessToken
}

func (api *API) CreateGoldenMacaroon() {
	api.goldenMacaroon = api.GetClientToken("")
}

func (api *API) GetClientToken(orgID string) []byte {
	reqURL := fmt.Sprintf("%s/generate-token", api.AdminURL)
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

func (api *API) CreateOrg() string {
	orgBundleFile, _ := os.Open("../../src/main/resources/organization_bundle_parameters.json")
	defer orgBundleFile.Close()
	orgBundleReader := bufio.NewReader(orgBundleFile)
	req, err := http.NewRequest("POST", fmt.Sprintf("%s/Organization/$submit", api.URL), orgBundleReader)
	req.Header.Add("Content-Type", "application/fhir+json")
	req.Header.Add("Authorization", fmt.Sprintf("Bearer %s", api.goldenMacaroon))
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

func (api *API) GenerateKeyPairAndSignature() (string, *rsa.PrivateKey, string) {
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

	signature, err := rsa.SignPKCS1v15(rand.Reader, privKey, crypto.SHA256, snippetHash.Sum(nil))
	if err != nil {
		cleanAndPanic(err)
	}
	sigEnc := base64.StdEncoding.EncodeToString(signature)

	return pubKeyStr, privKey, sigEnc
}

func (api *API) UploadKey(key, sig, orgID string) string {
	keySigReader := strings.NewReader(fmt.Sprintf("{ \"key\": \"%s\", \"signature\": \"%s\" }", key, sig))
	req, err := http.NewRequest("POST", fmt.Sprintf("%s/upload-key?organization=%s", api.AdminURL, orgID), keySigReader)
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

func (api *API) DeleteOrg(orgID string) {
	req, err := http.NewRequest("DELETE", fmt.Sprintf("%s/Organization/%s", api.URL, orgID), nil)
	if err != nil {
		fmt.Println("Organization could not be deleted", err)
	}
	req.Header.Add("Authorization", fmt.Sprintf("Bearer %s", api.goldenMacaroon))
	_, err = http.DefaultClient.Do(req)
	if err != nil {
		fmt.Println("Organization could not be deleted", err)
	}

}

func (api *API) generateKeyBodies(n int, fn func() (string, *rsa.PrivateKey, string)) [][]byte {
	var bodies [][]byte
	for i := 0; i < n; i++ {
		pubKeyStr, _, signature := fn()
		bodies = append(bodies, []byte(fmt.Sprintf("{ \"key\": \"%s\", \"signature\": \"%s\"}", pubKeyStr, signature)))
	}
	return bodies
}

func (api *API) SetupOrgAuth(orgID string) (string, string, *rsa.PrivateKey, []byte) {
	pubKeyStr, privateKey, signature := api.GenerateKeyPairAndSignature()
	keyID := api.UploadKey(pubKeyStr, signature, orgID)
	clientToken := api.GetClientToken(orgID)
	accessToken := api.RefreshAccessToken(privateKey, keyID, clientToken)

	return accessToken, keyID, privateKey, clientToken
}
