package main

import (
	"dpcaws"
	"os"

	log "github.com/sirupsen/logrus"
)

var createConnection = dpcaws.CreateConnection

var getAttributionData = func(dbUser string, dbPassword string, patientInfos map[string]PatientInfo) error {
	db, attributionConnErr := createConnection(os.Getenv("DB_HOST"), "dpc_attribution", dbUser, dbPassword)
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

var getConsentData = func(dbUser string, dbPassword string, patientInfos map[string]PatientInfo) error {
	db, consentConnErr := createConnection(os.Getenv("DB_HOST"), "dpc_consent", dbUser, dbPassword)
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
