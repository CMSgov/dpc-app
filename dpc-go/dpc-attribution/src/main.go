package main

import (
	"context"
	"fmt"
	"github.com/CMSgov/dpc/attribution/conf"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/CMSgov/dpc/attribution/router"
	v2 "github.com/CMSgov/dpc/attribution/v2"
	"go.uber.org/zap"
	"net/http"
)

func main() {
	conf.NewConfig()
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

	or := repository.NewOrganizationRepo(db)
	os := v2.NewOrganizationService(or)

	gr := repository.NewGroupRepo(db)
	gs := v2.NewGroupService(gr)

	ir := repository.NewImplementerRepo(db)
	is := v2.NewImplementerService(ir)

    ior := repository.NewImplementerOrgRepo(db)
	ios := v2.NewImplementerOrgService(ir, or, ior)

	attributionRouter := router.NewDPCAttributionRouter(os, gs, is, ios)

	port := conf.GetAsString("port", "3001")
	fmt.Printf("Starting DPC-Attribution server on port %v ...\n", port)
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), attributionRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}
