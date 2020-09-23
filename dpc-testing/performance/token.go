package main

import (
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
	postTokenTarget := vegeta.Target{
		Method: "POST",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Token"),
		Header: map[string][]string{
			"Content-Type":  {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessToken)},
		},
	}
	resps := runTest(postTokenTarget, 5, 5)
	for _, resp := range resps {
		var result ClientTokenResp
		json.Unmarshal(resp, &result)
		clientTokenResps = append(clientTokenResps, result)
	}

	postTokenAuthTargeter := newPOSTTokenAuthTargeter(func() string {
		authToken, err := dpcclient.GenerateAuthToken(privateKey, keyID, clientTokenResps[0].ClientToken, apiURL)
		if err != nil {
			cleanAndPanic(err)
		}
		return string(authToken)
	})
	resps = runTestWithTargeter(fmt.Sprintf("%s/Token/auth", apiURL), postTokenAuthTargeter, 5, 5)
	var accessTokens []string
	for _, resp := range resps {
		var result AccessTokenResp
		json.Unmarshal(resp, &result)
		accessTokens = append(accessTokens, result.AccessToken)
	}

	getTokensTarget := vegeta.Target{
		Method: "GET",
		URL:    fmt.Sprintf("%s%s", apiURL, "/Token"),
		Header: map[string][]string{
			"Accept":        {"application/json"},
			"Authorization": {fmt.Sprintf("Bearer %s", accessTokens[0])},
		},
	}

	runTest(getTokensTarget, 5, 5)
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
