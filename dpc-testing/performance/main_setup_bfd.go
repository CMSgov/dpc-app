package main

import (
	"flag"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc"
)

func main() {
	var apiURL, adminURL, orgID string
	var numOfPatients int

	flag.StringVar(&apiURL, "api_url", "http://localhost:3002/v1", "Base URL of API")
	flag.StringVar(&adminURL, "admin_url", "http://localhost:9903/tasks", "Base URL of admin tasks")
	flag.StringVar(&orgID, "org_id", "", "org id to use for creation")
	flag.IntVar(&numOfPatients, "num_of_patients", 3000, "The number of patients to create")
	flag.Parse()

	api := dpc.New(apiURL, dpc.AdminAPI{URL: adminURL})

	dpc.CreateDirs()
	defer dpc.DeleteDirs()

	// Run Perfomance test files
	api.RunTestSetup(orgID, numOfPatients)
}
