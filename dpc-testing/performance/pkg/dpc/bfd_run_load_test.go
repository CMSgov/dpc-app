package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunLoadTest(orgID string, retries int) {

	// Create organization (and delete at the end) and setup accesstoken
	auth := api.SetUpOrgAuth(orgID)
	defer api.DeleteOrg(auth.orgID)

	// Get Group
	resps, _ := targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    "Group",
		AccessToken: auth.accessToken,
	}).Run(1, 1)

	grpIDs := unmarshalIDs(resps)

	// Export Group
	_, headers := targeter.New(targeter.Config{
		Method:      "GET",
		BaseURL:     api.URL,
		Endpoint:    "Group",
		IDs:         grpIDs,
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
