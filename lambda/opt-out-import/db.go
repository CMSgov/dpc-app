package main

import (
	"context"
	"database/sql"
	"fmt"
	"os"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/ssm"
	"github.com/google/uuid"
	_ "github.com/lib/pq"
	log "github.com/sirupsen/logrus"
	"github.com/aws/smithy-go/logging"
)

const (
	Accepted = "Accepted"
	Rejected = "Rejected"
)

func getConsentDbSecrets(ctx context.Context, dbuser string, dbpassword string) (map[string]string, error) {
	var secretsInfo map[string]string = make(map[string]string)
	if isTesting {
		secretsInfo[dbuser] = os.Getenv("DB_USER_DPC_CONSENT")
		secretsInfo[dbpassword] = os.Getenv("DB_PASS_DPC_CONSENT")
	} else {
		var keynames []string = make([]string, 2)
		keynames[0] = dbuser
		keynames[1] = dbpassword

		cfg, err := config.LoadDefaultConfig(ctx,
			config.WithRegion("us-east-1"),
			config.WithLogger(logging.Nop{}),
		)
		if err != nil {
			return nil, fmt.Errorf("getConsentDbSecrets: Error creating AWS session: %w", err)
		}
		ssmsvc := ssm.NewFromConfig(cfg)

		withDecryption := true
		params, err := ssmsvc.GetParameters(ctx, &ssm.GetParametersInput{
			Names:          keynames,
			WithDecryption: &withDecryption,
		})
		if err != nil {
			return nil, fmt.Errorf("getConsentDbSecrets: Error connecting to parameter store: %w", err)
		}

		// Unknown keys will come back as invalid, make sure we error on them
		if len(params.InvalidParameters) > 0 {
			invalidParamsStr := ""
			for i := 0; i < len(params.InvalidParameters); i++ {
				invalidParamsStr += fmt.Sprintf("%s,\n", params.InvalidParameters[i])
			}
			return nil, fmt.Errorf("invalid parameters error: %s", invalidParamsStr)
		}

		for _, item := range params.Parameters {
			secretsInfo[*item.Name] = *item.Value
		}
	}

	return secretsInfo, nil
}

func getAssumeRoleArn(ctx context.Context, cfg aws.Config) (string, error) {
	if isTesting {
		val := os.Getenv("AWS_ASSUME_ROLE_ARN")
		if val == "" {
			return "", fmt.Errorf("AWS_ASSUME_ROLE_ARN must be set during testing")
		}

		return val, nil
	}

	parameterName := fmt.Sprintf("/opt-out-import/dpc/%s/bfd-bucket-role-arn", os.Getenv("ENV"))

	var keynames []*string = make([]*string, 1)
	keynames[0] = &parameterName

	ssmsvc := ssm.NewFromConfig(cfg)

	withDecryption := true
	result, err := ssmsvc.GetParameter(ctx, &ssm.GetParameterInput{
		Name:           &parameterName,
		WithDecryption: &withDecryption,
	})

	if err != nil {
		return "", fmt.Errorf("getAssumeRoleArn: Error connecting to parameter store: %w", err)
	}

	arn := *result.Parameter.Value

	if arn == "" {
		return "", fmt.Errorf("getAssumeRoleArn: No value found for bfd-bucket-role-arn")
	}

	return arn, nil
}

func insertResponseFileMetadata(db *sql.DB, optOutMetadata *ResponseFileMetadata) (OptOutFileEntity, error) {
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

func insertConsentRecords(db *sql.DB, optOutFileId string, records []*OptOutRecord) ([]*OptOutRecord, error) {
	createdRecords := []*OptOutRecord{}

	// If there aren't any rows, skip this and update the import_status of the file
	if len(records) > 0 {
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
		query += "RETURNING id, mbi, effective_date, policy_code, opt_out_file_id"

		rows, err := db.Query(query)
		if err != nil {
			if err := updateResponseFileImportStatus(db, optOutFileId, ImportFail); err != nil {
				return createdRecords, fmt.Errorf(
					"insertConsentRecords: failed to update opt_out_file status to Failed: %w", err)
			}
			return createdRecords, fmt.Errorf("insertConsentRecords: failed to insert to consent table: %w", err)
		}

		for rows.Next() {
			record := OptOutRecord{}
			if err := rows.Scan(&record.ID, &record.MBI, &record.EffectiveDt, &record.PolicyCode, &record.OptOutFileID); err != nil {
				return createdRecords, fmt.Errorf("insertConsentRecords: Failed to read newly created consent records: %w", err)
			}
			record.Status = Accepted
			createdRecords = append(createdRecords, &record)
		}

		// We're inserting all records in one batch, so if there wasn't an error they were all processed successfully
		for _, record := range records {
			record.Status = Accepted
		}

		log.Info("Successfully inserted consent records.")
	} else {
		log.Info("No consent records to insert.")
	}

	err := updateResponseFileImportStatus(db, optOutFileId, ImportComplete)
	if err != nil {
		return createdRecords, fmt.Errorf(
			"insertConsentRecords: failed to update opt_out_file status to Complete: %w", err)
	}
	return createdRecords, err
}

func updateResponseFileImportStatus(db *sql.DB, optOutFileId string, status string) error {
	entity := &OptOutFileEntity{}
	query := `UPDATE opt_out_file
			  SET import_status = $1, updated_at = NOW()
			  WHERE id = $2
			  RETURNING id, import_status`
	row := db.QueryRow(query, status, optOutFileId)
	if err := row.Scan(&entity.id, &entity.import_status); err != nil {
		return fmt.Errorf("updateOptOutFileImportStatus: %w", err)
	}
	return nil
}

func createConnection(dbUser string, dbPassword string) (*sql.DB, error) {
	var dbName string = "dpc_consent"
	var dbHost string = os.Getenv("DB_HOST")
	var dbPort int = 5432
	var sslmode string = "require"
	if isTesting {
		sslmode = "disable"
	}
	psqlInfo := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=%s", dbHost, dbPort, dbUser, dbPassword, dbName, sslmode)

	db, err := sql.Open("postgres", psqlInfo)
	if err != nil {
		return db, fmt.Errorf("createConnection: %w", err)
	}

	return db, err
}
