package main

import (
	"database/sql"
	"fmt"
	"os"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/ssm"
	"github.com/google/uuid"
	_ "github.com/lib/pq"
	log "github.com/sirupsen/logrus"
)

const (
	Accepted = "Accepted"
	Rejected = "Rejected"
)

func getConsentDbSecrets(dbuser string, dbpassword string) (map[string]string, error) {
	var secretsInfo map[string]string = make(map[string]string)
	if isTesting {
		secretsInfo[dbuser] = os.Getenv("DB_USER_DPC_CONSENT")
		secretsInfo[dbpassword] = os.Getenv("DB_PASS_DPC_CONSENT")
	} else {
		var keynames []*string = make([]*string, 2)
		keynames[0] = &dbuser
		keynames[1] = &dbpassword

		sess, err := session.NewSession(&aws.Config{
			Region: aws.String("us-east-1"),
		})
		if err != nil {
			return nil, fmt.Errorf("getConsentDbSecrets: Error creating AWS session: %w", err)
		}
		ssmsvc := ssm.New(sess)

		withDecryption := true
		param, err := ssmsvc.GetParameters(&ssm.GetParametersInput{
			Names:          keynames,
			WithDecryption: &withDecryption,
		})
		if err != nil {
			return nil, fmt.Errorf("getConsentDbSecrets: Error connecting to parameter store: %w", err)
		}

		for _, item := range param.Parameters {
			secretsInfo[*item.Name] = *item.Value
		}
	}

	return secretsInfo, nil
}

func insertOptOutMetadata(db *sql.DB, optOutMetadata *OptOutFilenameMetadata) (OptOutFileEntity, error) {
	optOutFile := &OptOutFileEntity{}
	id := uuid.New().String()
	optOutMetadata.FileID = id
	query := `INSERT INTO opt_out_file (id, name, timestamp, import_status, created_at, updated_at) 
		VALUES ($1, $2, $3, 'In-Progress', NOW(), NOW())
		RETURNING id, name, timestamp, import_status`
	row := db.QueryRow(query, id, optOutMetadata.Name, optOutMetadata.Timestamp.Format(time.DateOnly))
	if err := row.Scan(&optOutFile.id, &optOutFile.name, &optOutFile.timestamp, &optOutFile.import_status); err != nil {
		return *optOutFile, fmt.Errorf("insertOptOutMetadata: Query error: %w", err)
	}

	log.Info("Successfully imported opt out file metadata.")
	return *optOutFile, nil
}

func insertConsentRecords(db *sql.DB, optOutFileId string, records []*OptOutRecord) ([]OptOutRecord, error) {
	createdRecords := []OptOutRecord{}
	query := `INSERT INTO consent (id, mbi, effective_date, policy_code, loinc_code, opt_out_file_id, created_at, updated_at) 
			  VALUES `
	for i, rec := range records {
		query += fmt.Sprintf("('%s', '%s', NOW()::date, '%s', '64292-6', '%s', 'NOW()', 'NOW()')",
			rec.ID, rec.MBI, rec.PolicyCode, optOutFileId)
		if i < len(records)-1 {
			query += ", "
		} else {
			query += "\n"
		}
	}
	query += "RETURNING id, mbi, effective_date, opt_out_file_id"

	rows, err := db.Query(query)
	if err != nil {
		if err := updateOptOutFileImportStatus(db, optOutFileId, ImportFail); err != nil {
			return createdRecords, fmt.Errorf(
				"insertConsentRecords: failed to update opt_out_file status to Failed: %w", err)
		}
		return createdRecords, fmt.Errorf("insertConsentRecords: failed to insert to consent table: %w", err)
	}
	for rows.Next() {
		record := OptOutRecord{}
		if err := rows.Scan(&record.ID, &record.MBI, &record.EffectiveDt, &record.OptOutFileID); err != nil {
			return createdRecords, fmt.Errorf("insertConsentRecords: Failed to read newly created consent records: %w", err)
		}
		record.Status = Accepted
		createdRecords = append(createdRecords, record)
	}

	// We're inserting all records in one batch, so if there wasn't an error they were all processed successfully
	for _, record := range records {
		record.Status = Accepted
	}

	log.Info("Successfully inserted consent records.")
	if err := updateOptOutFileImportStatus(db, optOutFileId, ImportComplete); err != nil {
		return createdRecords, fmt.Errorf(
			"insertConsentRecords: failed to update opt_out_file status to Complete: %w", err)
	}
	return createdRecords, err
}

func updateOptOutFileImportStatus(db *sql.DB, optOutFileId string, status string) error {
	entity := &OptOutFileEntity{}
	query := `UPDATE opt_out_file
			  SET import_status = $1, updated_at = NOW()
			  WHERE id = $2
			  RETURNING id, import_status, updated_at`
	row := db.QueryRow(query, status, optOutFileId)
	if err := row.Scan(&entity.id, &entity.import_status, &entity.updated_at); err != nil {
		return fmt.Errorf("updateOptOutFileImportStatus: %w", err)
	}
	return nil
}

func createConnection(dbUser string, dbPassword string) (*sql.DB, error) {
	var dbName string = "dpc_consent"
	var dbHost string = os.Getenv("DB_HOST")
	var dbPort int = 5432
	psqlInfo := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable", dbHost, dbPort, dbUser, dbPassword, dbName)

	db, err := sql.Open("postgres", psqlInfo)
	if err != nil {
		return db, fmt.Errorf("createConnection: %w", err)
	}

	return db, err
}
