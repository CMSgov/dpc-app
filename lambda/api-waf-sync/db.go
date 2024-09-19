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

var getAuthData = func(dbUser string, dbPassword string, ipAddresses []string) error {
	db, authConnErr := createConnection("dpc_auth", dbUser, dbPassword)
	if authConnErr != nil {
		return authConnErr
	}
	defer db.Close()

	rows, err := db.Query(`SELECT ip_address FROM ip_addresses`)
	if err != nil {
		log.Warningf("Error running query: %v", err)
		return err
	}

	count := 0
	defer rows.Close()
	for rows.Next() {
		var ip string

		err = rows.Scan(ip)
		if err != nil {
			log.Warningf("Error reading data: %v", err)
		} else {
			count += 1
			if count%10000 == 0 {
				log.Infof("Read %d rows", count)
			}
		}

		ipAddresses = append(ipAddresses, ip)
	}
	log.WithField("num_rows_scanned", count).Info("Successfully retrieved ip address data")
	return nil
}
