package main

import (
	"flag"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc"
)

func main() {
	var apiURL, adminURL string

	flag.StringVar(&apiURL, "api_url", "http://localhost:3002/v1", "Base URL of API")
	flag.StringVar(&adminURL, "admin_url", "http://localhost:9903/tasks", "Base URL of admin tasks")
	flag.Parse()

	api := dpc.New(apiURL, dpc.AdminAPI{URL: adminURL})

	dpc.CreateDirs()
	defer dpc.DeleteDirs()

	// Run Perfomance test files
	api.RunMetadataTests()
	api.RunKeyTests()
	api.RunTokenTests()
	api.RunPatientTests()
	api.RunPractitionerTests()
	api.RunOrgTests()
	api.RunGroupTests()
}
