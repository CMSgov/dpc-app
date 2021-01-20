package dpc

import (
	"flag"
	"fmt"
	"os"
	"testing"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
)

var apiURL, adminURL, orgID string
var retries, numOfPatients int

func TestMain(m *testing.M) {
	flag.StringVar(&apiURL, "api_url", "http://localhost:3002/v1", "Base URL of API")
	flag.StringVar(&adminURL, "admin_url", "http://localhost:9903/tasks", "Base URL of admin tasks")
	flag.StringVar(&orgID, "org_id", "", "Existing ORG ID to use to get the access token")
	flag.IntVar(&numOfPatients, "num_of_patients", 100, "The number of patients to create")
	flag.IntVar(&retries, "retries", 10, "Number of retries while waiting for the job to complete")

	flag.Parse()
	os.Exit(m.Run())
}

func BenchmarkExport(b *testing.B) {

	api := New(apiURL, AdminAPI{URL: adminURL})

	CreateDirs()

	createdOrgID := api.CreateOrg(orgID, "../../templates/organization-bundle-template.json")
	auth := api.SetUpOrgAuth(createdOrgID)

	// Create Practitioner
	resps, _ := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    "Practitioner",
		AccessToken: auth.accessToken,
		Generator:   templateBodyGenerator("../../templates/practitioner-template.json", map[string]func() string{"{NPI}": generateNPI}),
	}).Run(1, 1)

	pracIDs := unmarshalIDs(resps)

	var xProvValues []string
	for _, id := range pracIDs {
		xProvValues = append(xProvValues, fmt.Sprintf("{ \"resourceType\": \"Provenance\", \"meta\": { \"profile\": [ \"https://dpc.cms.gov/api/v1/StructureDefinition/dpc-profile-attestation\" ] }, \"recorded\": \"1990-01-01T00:00:00.000-05:00\", \"reason\": [ { \"system\": \"http://hl7.org/fhir/v3/ActReason\", \"code\": \"TREAT\" } ], \"agent\": [ { \"role\": [ { \"coding\": [ { \"system\": \"http://hl7.org/fhir/v3/RoleClass\", \"code\": \"AGNT\" } ] } ], \"whoReference\": { \"reference\": \"Organization/%s}}\" }, \"onBehalfOfReference\": { \"reference\": \"Practitioner/%s\" } } ] }", auth.orgID, id))
	}

	npis := unmarshalIdentifiers(resps, "http://hl7.org/fhir/sid/us-npi")

	// Create patients
	resps, _ = targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    "Patient",
		AccessToken: auth.accessToken,
		Generator:   templateBodyGenerator("../../templates/patient-template.json", map[string]func() string{"{MBI}": generateMBIFromFile("../../data/mbis.csv")}),
	}).Run(1, 1)

	patientIDs := unmarshalIDs(resps)

	// Create Group with Patient References
	resps, _ = targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    "Group",
		AccessToken: auth.accessToken,
		Headers: &targeter.Headers{
			ContentType: FHIR,
			Accept:      FHIR,
			Custom: map[string][]string{
				"X-Provenance": xProvValues,
			},
		},
		Generator: templateBodyGenerator("../../templates/group-with-patients-template.json", map[string]func() string{"{NPI}": targeter.GenStrs(npis), "{patients}": generatePatientEntity(patientIDs, "../../templates/patient-entity-template.json")}),
	}).Run(1, 1)

	grpID := unmarshalIDs(resps)[0]

	b.Run("Patients", func(b *testing.B) {
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
	})

	DeleteDirs()
	api.DeleteOrg(auth.orgID)
}
