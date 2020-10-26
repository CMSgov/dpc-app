package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunKeyTests() {
	const ENDPOINT = "Key"

	// Create organization (and delete at the end) and setup accesstoken
	auth := api.SetupOrgAuth()
	defer api.DeleteOrg(auth.orgID)

	// POST /Key
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: auth.accessToken,
		Bodies:      generateKeyBodies(25, api.GenerateKeyPairAndSignature),
		Headers:     Headers(JSON, UNSET),
	}).Run(5, 5)

	keyIDs := unmarshalIDs(resps)

	// GET /Key
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: auth.accessToken,
		Headers:     Headers(UNSET, JSON),
	}).Run(5, 5)

	// GET /Key/{id}
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: auth.accessToken,
		Headers:     Headers(UNSET, JSON),
		IDs:         keyIDs,
	}).Run(5, 5)

	// DELETE /Key/{id}
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: auth.accessToken,
		Headers:     Headers(UNSET, UNSET),
		IDs:         keyIDs,
	}).Run(5, 5)
}
