package repository

import (
	"context"
	"database/sql"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/jackc/pgx"
	"github.com/jackc/pgx/log/zapadapter"
	"github.com/jackc/pgx/stdlib"
	"log"
	"os"
)

var LogFatal = log.Fatal

func GetDbConnection() *sql.DB {
	dbURL, found := os.LookupEnv("ATTRIBUTION_DB_URL")
	if !found {
		dbURL = "postgresql://postgres:dpc-safe@localhost:5432/dpc_attribution"
	}
	dc := stdlib.DriverConfig{
		ConnConfig: pgx.ConnConfig{
			Logger:   zapadapter.NewLogger(logger.WithContext(context.Background())),
			LogLevel: pgx.LogLevelError,
		},
		AfterConnect: func(c *pgx.Conn) error {
			// Can be used to ensure temp tables, indexes, etc. exist
			return nil
		},
	}

	stdlib.RegisterDriverConfig(&dc)

	db, err := sql.Open("pgx", dc.ConnectionString(dbURL))
	if err != nil {
		LogFatal(err)
	}

	pingErr := db.Ping()
	if pingErr != nil {
		LogFatal(pingErr)
	}

	return db
}
