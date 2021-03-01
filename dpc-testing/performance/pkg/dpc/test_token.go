package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
	dpcclient "github.com/CMSgov/dpc-app/dpcclient/lib"
)

func (api *API) RunTokenTests() {
	const endpoint = "Token"

	// Create organization (and delete at the end) and setup accesstoken
	auth := api.SetUpOrgAuth()
	defer api.DeleteOrg(auth.orgID)

	// POST /Token
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		Generator:   keyBodyGenerator(25, api.GenerateKeyPairAndSignature),
		Headers:     &targeter.Headers{ContentType: JSON},
	}).Run(5, 5)

	clientTokens := unmarshalClientTokens(resps)
	clientTokenIDs := unmarshalIDs(resps)

	// Generate an auth token for each client token
	var authTokens [][]byte
	for _, ct := range clientTokens {
		authToken, err := dpcclient.GenerateAuthToken(auth.privateKey, auth.keyID, ct, api.URL)
		if err != nil {
			cleanAndPanic(err)
		}
		authTokens = append(authTokens, authToken)
	}

	// POST /Token/validate
	targeter.New(targeter.Config{
		Method:    "POST",
		BaseURL:   api.URL,
		Endpoint:  endpoint + "/validate",
		Generator: byteArrayGenerator(authTokens),
		Headers:   &targeter.Headers{ContentType: Plain},
	}).Run(5, 5)

	// POST /Token/auth
	resps = targeter.New(targeter.Config{
		Method:    "POST",
		BaseURL:   api.URL,
		Endpoint:  endpoint + "/auth",
		Generator: authBodyGenerator(authTokens),
		Headers:   &targeter.Headers{ContentType: Form},
	}).Run(5, 5)

	accessTokens := unmarshalAccessTokens(resps)

	// GET /Token
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: accessTokens[0], // Targeter cannot iterate over a set of tokens; just use the first
		Headers:     &targeter.Headers{Accept: JSON},
	}).Run(5, 5)

	// DELETE /Token/{id}
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		IDs:         clientTokenIDs,
		Headers:     &targeter.Headers{},
	}).Run(5, 5)
}
