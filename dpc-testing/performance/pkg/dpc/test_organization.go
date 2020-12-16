package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
	"github.com/joeljunstrom/go-luhn"
)

func (api *API) RunOrgTests() {
	const endpoint = "Organization"

	// POST
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint + "/$submit",
		AccessToken: string(api.goldenMacaroon),
		Generator: templateBodyGenerator("./templates/organization-bundle-template.json", map[string]func() string{"{NPI}": func() string {
			luhnWithPrefix := luhn.GenerateWithPrefix(15, "808403")
			return luhnWithPrefix[len(luhnWithPrefix)-10:]
		}}),
	}).Run(1, 1)

	orgIDs, _ := unmarshalIDs(resps)
	orgID := orgIDs[0]
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
		Generator: templateBodyGenerator("./templates/organization-template.json", map[string]func() string{"{NPI}": func() string {
			luhnWithPrefix := luhn.GenerateWithPrefix(15, "808403")
			return luhnWithPrefix[len(luhnWithPrefix)-10:]
		}}),
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
