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
	var sslmode string = "require"
	if isTesting {
		sslmode = "disable"
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

var getAuthData = func(dbUser string, dbPassword string) ([]string, error) {
	db, authConnErr := createConnection("dpc_auth", dbUser, dbPassword)
	if authConnErr != nil {
		return nil, authConnErr
	}
	defer db.Close()

	rows, queryErr := db.Query(`SELECT ip_address FROM ip_addresses`)
	if queryErr != nil {
		log.Warningf("Error running query: %v", queryErr)
		return nil, queryErr
	}

	count := 0
	ipAddresses := []string{}
	defer rows.Close()
	for rows.Next() {
		var ip string

		readErr := rows.Scan(&ip)
		if readErr != nil {
			log.Warningf("Error reading data: %v", readErr)
			return nil, readErr
		} else {
			count += 1
			if count%10000 == 0 {
				log.Infof("Read %d rows", count)
			}
		}

		ipAddresses = append(ipAddresses, fmt.Sprintf("%s/32", ip))
	}
	log.WithField("num_rows_scanned", count).Info("Successfully retrieved ip address data")
	return ipAddresses, nil
}
