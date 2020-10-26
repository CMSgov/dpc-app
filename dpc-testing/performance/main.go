package main

import (
	"flag"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc"
)

func main() {
	var api dpc.API

	flag.StringVar(&api.URL, "api_url", "http://localhost:3002/v1", "Base URL of API")
	flag.StringVar(&api.AdminURL, "admin_url", "http://localhost:9903/tasks", "Base URL of admin tasks")
	flag.Parse()

	api.CreateGoldenMacaroon()
	dpc.CreateDirs()
	defer dpc.DeleteDirs()

	orgID := api.CreateOrg()
	defer api.DeleteOrg(orgID)

	accessToken, keyID, privateKey, clientToken := api.SetupOrgAuth(orgID)

	// Run Perfomance test files
	api.RunMetadataTests()
	api.RunKeyTests(accessToken)
	api.RunTokenTests(accessToken, keyID, privateKey, clientToken)
	api.RunPatientTests(accessToken)
	api.RunOrgTests()
}
