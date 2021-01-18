package main

import (
	"flag"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc"
)

func main() {
	var apiURL, adminURL, orgID, clientToken string
	var numOfPatients, numOfRetries int

	flag.StringVar(&apiURL, "api_url", "http://localhost:3002/v1", "Base URL of API")
	flag.StringVar(&adminURL, "admin_url", "http://localhost:9903/tasks", "Base URL of admin tasks")
	flag.StringVar(&orgID, "org_id", "", "Existing ORG ID to use to get the access token")
	flag.StringVar(&clientToken, "client_token", "", "Client Token to use")
	flag.IntVar(&numOfPatients, "num_of_patients", 3000, "The number of patients to create")
	flag.IntVar(&numOfRetries, "num_of_retries", 30, "The number of retries to do while checking the job status")
	flag.Parse()

	api := dpc.New(apiURL, dpc.AdminAPI{})

	dpc.CreateDirs()
	defer dpc.DeleteDirs()

	// Run Perfomance test files
	api.RunBFDLoadTest(orgID, clientToken, numOfPatients, numOfRetries)
}
