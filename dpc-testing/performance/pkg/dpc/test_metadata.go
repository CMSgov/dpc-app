package dpc

import (
	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

func (api *API) RunMetadataTests() {
	ENDPOINT := "metadata"

	targeter.New(targeter.Config{
		Method:   "GET",
		BaseURL:  api.URL,
		Endpoint: ENDPOINT,
	}).Run(5, 5)
}
