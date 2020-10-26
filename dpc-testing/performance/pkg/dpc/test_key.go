package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunKeyTests(accessToken string) {
	const ENDPOINT = "Key"

	// POST /Key
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessToken,
		Bodies:      generateKeyBodies(25, api.GenerateKeyPairAndSignature),
		Headers:     Headers(JSON, UNSET),
	}).Run(5, 5)

	keyIDs := unmarshalIDs(resps)

	// GET /Key
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessToken,
		Headers:     Headers(UNSET, JSON),
	}).Run(5, 5)

	// GET /Key/{id}
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessToken,
		Headers:     Headers(UNSET, JSON),
		IDs:         keyIDs,
	}).Run(5, 5)

	// DELETE /Key/{id}
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessToken,
		Headers:     Headers(UNSET, UNSET),
		IDs:         keyIDs,
	}).Run(5, 5)
}
