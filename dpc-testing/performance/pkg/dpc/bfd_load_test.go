package dpc

import (
	"flag"
	"fmt"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/CMSgov/dpc-app/dpc-testing/performance/pkg/dpc/targeter"
	"github.com/go-pg/pg"
	"github.com/google/uuid"
)

var apiURL, adminURL, orgID, dbURL, dbUser, dbPass, tokenEnv string
var numOfPatients, numOfBatches int

type JobQueueBatch struct {
	tableName       struct{} `sql:"job_queue_batch"`
	JobID           string   `pg:"job_id"`
	BatchID         string   `pg:"batch_id"`
	OrganizationID  string   `pg:"organization_id"`
	ProviderID      string   `pg:"provider_id"`
	Patients        []string
	ResourceTypes   []string
	Status          int `sql:",notnull"`
	Priority        int
	IsBulk          bool
	TransactionTime time.Time
}

func TestMain(m *testing.M) {
	flag.StringVar(&tokenEnv, "api_env", "dev", "The Token Environment")
	flag.StringVar(&apiURL, "api_url", "http://localhost:3002/v1", "Base URL of API")
	flag.StringVar(&adminURL, "admin_url", "http://localhost:9903/tasks", "Base URL of admin tasks")
	flag.StringVar(&dbURL, "db_url", "localhost:5432", "The database url")
	flag.StringVar(&dbUser, "db_user", "postgres", "The database username")
	flag.StringVar(&dbPass, "db_pass", "dpc-safe", "The database password")
	flag.IntVar(&numOfPatients, "num_of_patients", 10, "The number of patients to create")
	flag.IntVar(&numOfBatches, "num_of_batches", 3, "Number of batches to create")

	flag.Parse()
	os.Exit(m.Run())
}

func BenchmarkExport(b *testing.B) {

	api := New(apiURL, AdminAPI{URL: adminURL})
	if len(tokenEnv) > 0 {
		api.publicURL = fmt.Sprintf("https://%s.dpc.cms.gov/api/v1", tokenEnv)
	}

	CreateDirs()
	defer DeleteDirs()

	db := pg.Connect(&pg.Options{
		Addr:     dbURL,
		User:     dbUser,
		Password: dbPass,
		Database: "dpc_queue",
	})

	defer db.Close()

	// Create organization (and delete at the end) and setup accesstoken
	orgID := api.CreateOrgWithTemplate("../../templates/organization-bundle-template.json")
	auth := api.SetUpOrgAuth(orgID)
	defer api.DeleteOrg(auth.orgID)

	// Create Practitioner
	resps := targeter.New(targeter.Config{
		Method:      "POST",
		BaseURL:     api.URL,
		Endpoint:    "Practitioner",
		AccessToken: auth.accessToken,
		Generator:   templateBodyGenerator("../../templates/practitioner-template.json", map[string]func() string{"{NPI}": generateNPI}),
	}).Run(1, 1)

	pracIDs := unmarshalIDs(resps)

	b.Run("BFD Load", func(b *testing.B) {
		// Create job queue batches with patients in the queued state in one transaction
		jobID, createJobBatchesTx := createJobBatches(orgID, pracIDs[0], numOfPatients, numOfBatches)
		if err := db.RunInTransaction(createJobBatchesTx); err != nil {
			panic(err)
		}

		// Wait for job to finish
		var statuses []int
		for ok := true; ok; ok = !allComplete(statuses) {
			err := db.Model((*JobQueueBatch)(nil)).Column("status").Where("job_id = ?", jobID).Select(&statuses)
			if err != nil {
				panic(err)
			}
		}
	})
}

func createJobBatches(orgID string, pracID string, numOfPatients int, numOfBatches int) (string, func(tx *pg.Tx) error) {
	jobID := uuid.New().String()
	patientMBIGenerator := generateMBIFromFile("../../data/mbis.csv")

	txFn := func(tx *pg.Tx) error {
		for i := 0; i < numOfBatches; i++ {
			patients := []string{}
			for i := 0; i < numOfPatients; i++ {
				patients = append(patients, patientMBIGenerator())
			}
			jobQueueBatch := &JobQueueBatch{
				JobID:           jobID,
				BatchID:         uuid.New().String(),
				OrganizationID:  orgID,
				ProviderID:      pracID,
				Patients:        strings.Join(patients, ","),
				ResourceTypes:   "Patient,ExplanationOfBenefit,Coverage",
				Status:          0,
				Priority:        1000,
				IsBulk:          true,
				TransactionTime: time.Now().AddDate(-1, 0, 0),
			}
			_, err := tx.Model(jobQueueBatch).Insert()
			if err != nil {
				return err
			}
		}
		return nil
	}
	return jobID, txFn
}

func allComplete(statuses []int) bool {
	if len(statuses) == 0 {
		return false
	}
	for _, s := range statuses {
		if s < 2 {
			return false
		}

	}
	return true
}
