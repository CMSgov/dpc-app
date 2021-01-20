package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunKeyTests() {
	const endpoint = "Key"
	// Create organization (and delete at the end) and setup accesstoken
	auth := api.SetUpOrgAuth()
	defer api.DeleteOrg(auth.orgID)

	// POST /Key
	resps, _ := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		Generator:   keyBodyGenerator(25, api.GenerateKeyPairAndSignature),
		Headers:     &targeter.Headers{ContentType: JSON},
	}).Run(5, 5)

	keyIDs := unmarshalIDs(resps)

	// GET /Key
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		Headers:     &targeter.Headers{Accept: JSON},
	}).Run(5, 5)

	// GET /Key/{id}
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		Headers:     &targeter.Headers{Accept: JSON},
		IDs:         keyIDs,
	}).Run(5, 5)

	// DELETE /Key/{id}
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		Headers:     &targeter.Headers{},
		IDs:         keyIDs,
	}).Run(5, 5)
}
