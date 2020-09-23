package main

import (
	"container/list"
	"crypto/rsa"
	"encoding/json"
	"fmt"
	"net/url"

	dpcclient "github.com/CMSgov/dpc-app/dpcclient/lib"
	vegeta "github.com/tsenart/vegeta/lib"
)

type ClientTokenResp struct {
	Resource
	ClientToken []byte `json:"token"`
}

type AccessTokenResp struct {
	Resource
	AccessToken string `json:"access_token"`
}

func testTokenEndpoints(accessToken string, privateKey *rsa.PrivateKey, keyID string, clientToken []byte) {
	// POST /Token
	postTokenTarget := vegeta.Target{
		Method: "POST",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Token"),
		Header: map[string][]string{
			"Content-Type":  {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}
	resps := runTest(postTokenTarget, 5, 5)
	var clientTokenResps []ClientTokenResp
	for _, resp := range resps {
		var result ClientTokenResp
		json.Unmarshal(resp, &result)
		clientTokenResps = append(clientTokenResps, result)
	}

	// POST /Token/auth
	var authTokens = list.New()
	for i := 0; i < 25; i++ {
		authToken, err := dpcclient.GenerateAuthToken(privateKey, keyID, clientTokenResps[0].ClientToken, apiURL)
		if err != nil {
			cleanAndPanic(err)
		}
		authTokens.PushBack(authToken)
	}
	currentAuthToken := authTokens.Front()
	postTokenAuthTargeter := newPOSTTokenAuthTargeter(func() string {
		b, ok := currentAuthToken.Value.([]byte)
		if !ok {
			cleanAndPanic(fmt.Errorf("not a valid byte array: %v", currentAuthToken.Value))
		}
		currentAuthToken = currentAuthToken.Next()
		return string(b)
	})
	resps = runTestWithTargeter(fmt.Sprintf("%s/Token/auth", apiURL), postTokenAuthTargeter, 5, 5)
	var accessTokens []string
	for _, resp := range resps {
		var result AccessTokenResp
		json.Unmarshal(resp, &result)
		accessTokens = append(accessTokens, result.AccessToken)
	}

	// GET /Token
	getTokensTarget := vegeta.Target{
		Method: "GET",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Token"),
		Header: map[string][]string{
			"Accept":        {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessTokens[0])},
		},
	}
	runTest(getTokensTarget, 5, 5)

	// GET /Token/{id}
	getTokenTarget := vegeta.Target{
		Method: "GET",
		URL:    fmt.Sprintf("%s%s%s", apiURL, "/Token/", clientTokenResps[0].ID),
		Header: map[string][]string{
			"Accept":        {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessTokens[0])},
		},
	}
	runTest(getTokenTarget, 5, 5)

	// POST /Token/validate
	// currentAuthToken = authTokens.Front()
	// validateTokenTargeter := newPOSTTokenValidateTargeter(func() []byte {
	// 	b, ok := currentAuthToken.Value.([]byte)
	// 	if !ok {
	// 		cleanAndPanic(fmt.Errorf("not a valid byte array: %v", currentAuthToken.Value))
	// 	}
	// 	currentAuthToken = currentAuthToken.Next()
	// 	return b
	// })
	// runTestWithTargeter(fmt.Sprintf("POST %s/Token/validate", apiURL), validateTokenTargeter, 5, 5)

	// DELETE /Token/{id}
	// deleteTokensTargeter := newDELETETokenTargeter(func() string {
	// 	clientTokenResp := clientTokenResps[0]
	// 	clientTokenResps = clientTokenResps[1:]
	// 	return clientTokenResp.ID
	// }, accessToken)
	// runTestWithTargeter(fmt.Sprintf("DELETE %s/Token/{id}", apiURL), deleteTokensTargeter, 5, 5)
}

func newPOSTTokenAuthTargeter(nextAuthToken func() string) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "POST"
		t.URL = fmt.Sprintf("%s/Token/auth", apiURL)
		t.Header = map[string][]string{
			"Content-Type": {"application/x-www-form-urlencoded"},
		}

		v := url.Values{
			"scope":                 {"system/*.*"},
			"grant_type":            {"client_credentials"},
			"client_assertion_type": {"urn:ietf:params:oauth:client-assertion-type:jwt-bearer"},
			"client_assertion":      {nextAuthToken()},
		}
		t.Body = []byte(v.Encode())

		return nil
	}
}

func newPOSTTokenValidateTargeter(nextAuthToken func() []byte) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "POST"
		t.URL = fmt.Sprintf("%s/Token/validate", apiURL)
		t.Header = map[string][]string{
			"Content-Type": {"text/plain"},
		}
		t.Body = nextAuthToken()

		return nil
	}
}

func newDELETETokenTargeter(nextClientTokenID func() string, accessToken string) vegeta.Targeter {
	return func(t *vegeta.Target) error {
		t.Method = "DELETE"
		t.URL = fmt.Sprintf("%s/Token/%s", apiURL, nextClientTokenID())
		t.Header = map[string][]string{
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		}
		return nil
	}
}
