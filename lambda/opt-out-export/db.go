package main

import (
	"database/sql"
	"fmt"
	"os"

	log "github.com/sirupsen/logrus"
)

var createConnection = func(dbName string, dbUser string, dbPassword string) (*sql.DB, error) {
	var dbHost string = os.Getenv("DB_HOST")
	var dbPort int = 5432
	psqlInfo := fmt.Sprintf("host=%s port=%d user=%s password=%s dbname=%s sslmode=disable", dbHost, dbPort, dbUser, dbPassword, dbName)

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

var getAttributionData = func(dbUser string, dbPassword string, patientInfos map[string]PatientInfo) error {
	db, attributionConnErr := createConnection("dpc_attribution", dbUser, dbPassword)
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
		}

		patientInfos[perPatientInfo.beneficiary_id] = perPatientInfo
	}
	log.WithField("num_rows_scanned", count).Info("Successfully retrieved patient data")
	return nil
}

var getConsentData = func(dbUser string, dbPassword string, patientInfos map[string]PatientInfo) error {
	db, consentConnErr := createConnection("dpc_consent", dbUser, dbPassword)
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
