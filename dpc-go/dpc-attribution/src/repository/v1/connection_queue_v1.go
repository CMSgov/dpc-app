package v1

import (
	"context"
	"database/sql"
	"github.com/CMSgov/dpc/attribution/conf"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/huandu/go-sqlbuilder"
	"github.com/jackc/pgx"
	"github.com/jackc/pgx/log/zapadapter"
	"github.com/jackc/pgx/stdlib"
	"go.uber.org/zap"
)

const sqlFlavor = sqlbuilder.PostgreSQL

// GetQueueDbConnection function that sets up the dpc_attribution v1 db connection and returns the db struct
func GetQueueDbConnection() *sql.DB {
	log := logger.WithContext(context.Background())
	dbURL := conf.GetAsString("db.queueUrl")
	dc := stdlib.DriverConfig{
		ConnConfig: pgx.ConnConfig{
			Logger:   zapadapter.NewLogger(log),
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
		log.Fatal("Could not open a connection to the db", zap.Error(err))
	}

	pingErr := db.Ping()
	if pingErr != nil {
		log.Fatal("Could not ping the database", zap.Error(pingErr))
	}

	return db
}
