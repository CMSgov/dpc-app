package dpc

import (
	"bufio"
	"context"
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strings"

	dpcclient "github.com/CMSgov/dpc-app/dpcclient/lib"
	retryablehttp "github.com/hashicorp/go-retryablehttp"
)

type API struct {
	URL            string
	goldenMacaroon []byte
	AdminAPI
}

func New(apiURL string, admin AdminAPI) *API {
	api := API{
		URL:      apiURL,
		AdminAPI: admin,
	}
	if (AdminAPI{}) != admin {
		api.goldenMacaroon = admin.GetClientToken()
	}

	return &api
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

func (api *API) CreateOrg() string {
	orgBundleFile, _ := os.Open("../../src/main/resources/organization_bundle_parameters.json")
	defer orgBundleFile.Close()
	orgBundleReader := bufio.NewReader(orgBundleFile)
	req, err := http.NewRequest("POST", fmt.Sprintf("%s/Organization/$submit", api.URL), orgBundleReader)
	if err != nil {
		cleanAndPanic(err)
	}
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
	if result.Type == "OperationOutcome" {
		cleanAndPanic(errors.New(string(orgResp)))
	}
	return result.ID
}

func (api *API) GenerateKeyPairAndSignature() (string, *rsa.PrivateKey, string) {
	privKey, pubKey, err := dpcclient.GenRSAKeyPair()
	if err != nil {
		cleanAndPanic(err)
	}
	if err := dpcclient.SaveDPCKeyPair("./keys/dpc-key", privKey, pubKey); err != nil {
		cleanAndPanic(err)
	}

	pubKeyBytes, err := dpcclient.ReadSmallFile("./keys/dpc-key-public.pem")
	if err != nil {
		cleanAndPanic(err)
	}
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
	req, err := http.NewRequest("POST", fmt.Sprintf("%s/upload-key?organization=%s", api.AdminAPI.URL, orgID), keySigReader)
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

func (api *API) CheckJobStatus(url string, accessToken string, retries int) int {
	req, err := retryablehttp.NewRequest("GET", url, nil)
	if err != nil {
		cleanAndPanic(err)
	}
	req.Header.Add("Authorization", fmt.Sprintf("Bearer %s", accessToken))

	retryClient := retryablehttp.NewClient()
	retryClient.RetryMax = retries
	retryClient.CheckRetry = func(ctx context.Context, resp *http.Response, err error) (bool, error) {
		shouldRetry := resp.StatusCode == http.StatusAccepted
		return shouldRetry, nil
	}
	resp, err := retryClient.Do(req)
	if err != nil {
		cleanAndPanic(err)
	}
	return resp.StatusCode
}

type orgAuth struct {
	orgID       string
	accessToken string
	keyID       string
	privateKey  *rsa.PrivateKey
}

func (api *API) SetUpOrgAuth(orgIDs ...string) orgAuth {
	var orgID string
	if len(orgIDs) > 0 {
		orgID = orgIDs[0]
	} else {
		orgID = api.CreateOrg()
	}
	pubKeyStr, privateKey, signature := api.GenerateKeyPairAndSignature()
	keyID := api.UploadKey(pubKeyStr, signature, orgID)
	clientToken := api.AdminAPI.GetClientToken(orgID)
	accessToken := api.RefreshAccessToken(privateKey, keyID, clientToken)

	return orgAuth{
		orgID:       orgID,
		accessToken: accessToken,
		keyID:       keyID,
		privateKey:  privateKey,
	}
}

func (api *API) SetUpOrgAuthWith(orgID string, clientToken string) orgAuth {
	pubKeyStr, privateKey, signature := api.GenerateKeyPairAndSignature()
	keyID := api.UploadKey(pubKeyStr, signature, orgID)
	accessToken := api.RefreshAccessToken(privateKey, keyID, []byte(clientToken))

	return orgAuth{
		orgID:       orgID,
		accessToken: accessToken,
		keyID:       keyID,
		privateKey:  privateKey,
	}
}
