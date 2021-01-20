package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
	"github.com/google/uuid"
)

func (api *API) RunOrgTests() {
	const endpoint = "Organization"

	// POST
	resps, _ := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint + "/$submit",
		AccessToken: string(api.goldenMacaroon),
		Generator:   templateBodyGenerator("./templates/organization-bundle-template.json", map[string]func() string{"{NPI}": generateNPI, "{ID}": func() string { return uuid.New().String() }}),
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
		Generator:   templateBodyGenerator("./templates/organization-template.json", map[string]func() string{"{NPI}": generateNPI}),
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
