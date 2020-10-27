package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunOrgTests() {
	const endpoint = "Organization"

	// POST
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint + "/$submit",
		AccessToken: string(api.goldenMacaroon),
		Bodies:      readBodies("../../src/main/resources/organizations/base-organization.json"),
	}).Run(1, 1)

	orgID := unmarshalIDs(resps)[0]
	auth := api.SetUpOrgAuth(orgID)

	// GET
	// ids := []string{resource.ID}
	targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		ID:          orgID,
	}).Run(5, 2)

	// PUT
	targeter.New(targeter.Config{
		Method:      "PUT",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		ID:          orgID,
		AccessToken: auth.accessToken,
		Bodies:      readBodies("../../src/main/resources/organizations/organization-*.json"),
	}).Run(5, 2)

	// DELETE
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: string(api.goldenMacaroon),
		ID:          orgID,
	}).Run(1, 1)
}
