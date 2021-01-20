package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunLoad(orgID string, retries int) {

	// Create organization (and delete at the end) and setup accesstoken
	auth := api.SetUpOrgAuth(orgID)
	// defer api.DeleteOrg(auth.orgID)

	// Find Practitioner
	resps, _ := targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    "Practitioner",
		AccessToken: auth.accessToken,
	}).Run(1, 1)

	bundles := unmarshalBundle(resps)
	npi := bundles[0].Entry[0].Resource.Identifier[0].Value

	// Find Group
	resps, _ = targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    "Group?characteristic-value=attributed-to$" + npi,
		AccessToken: auth.accessToken,
	}).Run(1, 1)

	bundles = unmarshalBundle(resps)
	grpID := bundles[0].Entry[0].Resource.ID

	// Export Group
	_, headers := targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    "Group",
		ID:          grpID,
		Operation:   "$export",
		AccessToken: auth.accessToken,
		Headers: &targeter.Headers{
			ContentType: FHIR,
			Accept:      FHIR,
			Custom: map[string][]string{
				"Prefer": {"respond-async"},
			},
		},
	}).Run(1, 1)

	api.CheckJobStatus(headers[0].Get("Content-Location"), auth.accessToken, retries)
}
