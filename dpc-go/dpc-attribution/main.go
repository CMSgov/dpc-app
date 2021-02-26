package main

import (
	"context"
	"fmt"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/CMSgov/dpc/attribution/router"
	v2 "github.com/CMSgov/dpc/attribution/v2"
	"go.uber.org/zap"
	"net/http"
	"os"
)

func main() {
	ctx := context.Background()
	defer func() {
		err := logger.SyncLogger()
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}()
	db := repository.GetDbConnection()
	defer func() {
		if err := db.Close(); err != nil {
			logger.WithContext(ctx).Fatal("Failed to close db connection", zap.Error(err))
		}
	}()

	r := repository.NewOrganizationRepo(db)
	c := v2.NewOrganizationService(r)

	attributionRouter := router.NewDPCAttributionRouter(c)

	port := os.Getenv("ATTRIBUTION_PORT")
	if port == "" {
		port = "3001"
	}
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), attributionRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}
