package dpc

import (
	"crypto/rsa"
	"net/url"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
	dpcclient "github.com/CMSgov/dpc-app/dpcclient/lib"
)

func (api *API) RunTokenTests(accessToken, keyID string, privateKey *rsa.PrivateKey, clientToken []byte) {
	const ENDPOINT = "Token"

	// POST /Token
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessToken,
		Bodies:      generateKeyBodies(25, api.GenerateKeyPairAndSignature),
		Headers:     Headers(JSON, UNSET),
	}).Run(5, 5)

	clientTokens := unmarshalClientTokens(resps)
	clientTokenIDs := unmarshalIDs(resps)

	// Generate an auth token for each client token
	var authTokens [][]byte
	for _, ct := range clientTokens {
		authToken, err := dpcclient.GenerateAuthToken(privateKey, keyID, ct, api.URL)
		if err != nil {
			cleanAndPanic(err)
		}
		authTokens = append(authTokens, authToken)
	}

	// POST /Token/validate
	targeter.New(targeter.Config{
		Method:   "POST",
		BaseURL:  api.URL,
		Endpoint: ENDPOINT + "/validate",
		Bodies:   authTokens,
		Headers:  Headers(PLAIN, UNSET),
	}).Run(5, 5)

	// POST /Token/auth
	resps = targeter.New(targeter.Config{
		Method:   "POST",
		BaseURL:  api.URL,
		Endpoint: ENDPOINT + "/auth",
		Bodies:   generateAuthBodies(authTokens),
		Headers:  Headers(FORM, UNSET),
	}).Run(5, 5)

	accessTokens := unmarshalAccessTokens(resps)

	// GET /Token
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessTokens[0], // Targeter cannot iterate over a set of tokens; just use the first
		Headers:     Headers(UNSET, JSON),
	}).Run(5, 5)

	// DELETE /Token/{id}
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessToken,
		IDs:         clientTokenIDs,
		Headers:     Headers(UNSET, UNSET),
	}).Run(5, 5)
}

func generateAuthBodies(authTokens [][]byte) [][]byte {
	var bodies [][]byte
	for _, authToken := range authTokens {
		bodies = append(bodies, []byte(
			url.Values{
				"scope":                 {"system/*.*"},
				"grant_type":            {"client_credentials"},
				"client_assertion_type": {"urn:ietf:params:oauth:client-assertion-type:jwt-bearer"},
				"client_assertion":      {string(authToken)},
			}.Encode()),
		)
	}

	return bodies
}
