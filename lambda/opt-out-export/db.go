package main

import (
	"context"
	"database/sql"
	"fmt"
	"os"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/feature/rds/auth"
	log "github.com/sirupsen/logrus"
)

var createConnection = func(ctx context.Context, cfg aws.Config, dbName string) (*sql.DB, error) {
	var dbHost string = os.Getenv("DB_HOST")
	var dbPort int = 5431
	var sslmode string
	var dbUser string
	var dbPassword string
	var err error
	if isTesting {
		sslmode = "disable"
		dbUser = "postgres"
		dbPassword = "dpc-safe"
	} else {
		sslmode = "require"
		dbUser = fmt.Sprintf("%s-%s-role", os.Getenv("ENV"), dbName)
		dbPassword, err = buildToken(ctx, cfg, dbHost, dbPort, dbUser)
		if err != nil {
			log.Warning("Error building IAM token")
			return nil, err
		}
	}
	psqlInfo := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=%s", dbHost, dbPort, dbUser, dbPassword, dbName, sslmode)

	db, err := sql.Open("postgres", psqlInfo)
	if err != nil {
		log.Warning("Error connecting to database")
		return db, err
	}
	// Call db.Ping() to check the connection
	pingErr := db.Ping()
	if pingErr != nil {
		log.Warning("Ping error")
		return db, pingErr
	}
	log.Info("Connected!")

	return db, nil
}

var buildToken = func(ctx context.Context, cfg aws.Config, dbHost string, dbPort int, dbUser string) (string, error) {
	return auth.BuildAuthToken(
		ctx,
		fmt.Sprintf("%s:%d", dbHost, dbPort),
		"us-east-1",
		dbUser,
		cfg.Credentials,
	)
}

var getAttributionData = func(ctx context.Context, cfg aws.Config, patientInfos map[string]PatientInfo) error {
	db, attributionConnErr := createConnection(ctx, cfg, "dpc_attribution")
	if attributionConnErr != nil {
		return attributionConnErr
	}
	defer db.Close()

	rows, err := db.Query(`SELECT DISTINCT beneficiary_id, first_name, last_name, dob FROM patients`)
	if err != nil {
		log.Warningf("Error running query: %v", err)
		return err
	}

	count := 0
	defer rows.Close()
	for rows.Next() {
		var perPatientInfo PatientInfo

		err = rows.Scan(&perPatientInfo.beneficiary_id, &perPatientInfo.first_name, &perPatientInfo.last_name, &perPatientInfo.dob)
		if err != nil {
			log.Warningf("Error reading data: %v", err)
		} else {
			count += 1
			if count%10000 == 0 {
				log.Infof("Read %d rows", count)
			}
		}

		patientInfos[perPatientInfo.beneficiary_id] = perPatientInfo
	}
	log.WithField("num_rows_scanned", count).Info("Successfully retrieved patient data")
	return nil
}

var getConsentData = func(ctx context.Context, cfg aws.Config, patientInfos map[string]PatientInfo) error {
	db, consentConnErr := createConnection(ctx, cfg, "dpc_consent")
	if consentConnErr != nil {
		return consentConnErr
	}
	defer db.Close()

	rows, err := db.Query(`
		SELECT DISTINCT ON (mbi) mbi, policy_code, effective_date 
		FROM consent
		ORDER BY mbi, effective_date DESC, created_at DESC
	`)
	if err != nil {
		log.Warningf("Error running query: %v", err)
		return err
	}

	count := 0
	defer rows.Close()
	for rows.Next() {
		var perPatientInfo PatientInfo

		err = rows.Scan(&perPatientInfo.beneficiary_id, &perPatientInfo.policy_code, &perPatientInfo.effective_date)
		if err != nil {
			log.Warningf("Error reading data: %v", err)
		} else {
			count += 1
		}
		entry, ok := patientInfos[perPatientInfo.beneficiary_id]
		if ok {
			entry.policy_code = perPatientInfo.policy_code
			entry.effective_date = perPatientInfo.effective_date
			patientInfos[perPatientInfo.beneficiary_id] = entry
		}
	}
	log.WithField("num_rows_scanned", count).Info("Successfully retrieved consent data")
	return nil
}
