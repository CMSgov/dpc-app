package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunPractitionerTests() {
	const endpoint = "Practitioner"

	// Create organization (and delete at the end) and setup accesstoken
	auth := api.SetUpOrgAuth()
	defer api.DeleteOrg(auth.orgID)

	// POST /Practitioner/$validate
	targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint + "/$validate",
		AccessToken: auth.accessToken,
		Generator:   templateBodyGenerator("./templates/practitioner-bundle-template.json", map[string]func() string{"{NPI}": generateNPI}),
	}).Run(5, 2)

	// POST /Practitioner
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		Generator:   templateBodyGenerator("./templates/practitioner-template.json", map[string]func() string{"{NPI}": generateNPI}),
	}).Run(5, 2)

	// Retrieve practitioner IDs which are required by the remaining tests
	pracIDs, _ := unmarshalIDs(resps)

	// POST /Practitioner/$submit
	targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    endpoint + "/$submit",
		AccessToken: auth.accessToken,
		Generator:   templateBodyGenerator("./templates/practitioner-bundle-template.json", map[string]func() string{"{NPI}": generateNPI}),
	}).Run(5, 2)

	// PUT /Practitioner/{id}
	targeter.New(targeter.Config{
		Method:      "PUT",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		IDs:         pracIDs,
		Generator:   templateBodyGenerator("./templates/practitioner-template.json", map[string]func() string{"{NPI}": generateNPI}),
	}).Run(5, 2)

	// DELETE /Practitioner/{id}
	targeter.New(targeter.Config{
		Method:      "DELETE",
		BaseURL:     api.URL,
		Endpoint:    endpoint,
		AccessToken: auth.accessToken,
		IDs:         pracIDs,
	}).Run(5, 2)
}
