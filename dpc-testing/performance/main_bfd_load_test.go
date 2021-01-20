package main

import (
	"flag"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc"
)

func main() {
	var apiURL, adminURL, orgID string
	var retries int

	flag.StringVar(&apiURL, "api_url", "http://localhost:3002/v1", "Base URL of API")
	flag.StringVar(&adminURL, "admin_url", "http://localhost:9903/tasks", "Base URL of admin tasks")
	flag.StringVar(&orgID, "org_id", "", "Existing ORG ID to use to get the access token")
	flag.IntVar(&retries, "retries", 10, "Number of retries while waiting for the job to complete")

	flag.Parse()

	api := dpc.New(apiURL, dpc.AdminAPI{URL: adminURL})

	dpc.CreateDirs()
	defer dpc.DeleteDirs()

	// Run Perfomance test files
	api.RunLoadTest(orgID, retries)
}
