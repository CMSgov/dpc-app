package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunOrgTests() {
	const ENDPOINT = "Organization"

	// POST
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT + "/$submit",
		AccessToken: string(api.goldenMacaroon),
		Bodies:      readBodies("../../src/main/resources/organizations/base-organization.json"),
	}).Run(1, 1)

	orgID := unmarshalIDs(resps)[0]
	accessToken, _, _, _ := api.SetupOrgAuth(orgID)

	// GET
	// ids := []string{resource.ID}
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: accessToken,
		ID:          orgID,
	}).Run(5, 2)

	// PUT
	targeter.New(targeter.Config{
		Method:      "PUT",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		ID:          orgID,
		AccessToken: accessToken,
		Bodies:      readBodies("../../src/main/resources/organizations/organization-*.json"),
	}).Run(5, 2)

	// DELETE
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    ENDPOINT,
		AccessToken: string(api.goldenMacaroon),
		ID:          orgID,
	}).Run(1, 1)
}
