package orm

import (
	"github.com/go-pg/pg/extra/pgdebug"
	"github.com/go-pg/pg/v10"
	"os"
)

func GetDbConnection() *pg.DB {
	dbURL, found := os.LookupEnv("ATTRIBUTION_DB_URL")
	if !found {
		dbURL = "localhost:5432"
	}
	dbUser, found := os.LookupEnv("ATTRIBUTION_DB_USERNAME")
	if !found {
		dbUser = "postgres"
	}
	dbPass, found := os.LookupEnv("ATTRIBUTION_DB_PASSWORD")
	if !found {
		dbPass = "dpc-safe"
	}
	db := pg.Connect(&pg.Options{
		Addr:     dbURL,
		User:     dbUser,
		Password: dbPass,
		Database: "dpc_attribution",
	})
	db.AddQueryHook(pgdebug.DebugHook{
		Verbose: false,
	})
	return db
}
