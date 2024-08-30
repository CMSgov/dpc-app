package dpcaws

import (
	"database/sql"
	"fmt"
)

func CreateConnection(dbHost, dbName, dbUser, dbPassword string) (*sql.DB, error) {
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
