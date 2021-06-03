package main

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/CMSgov/dpc/attribution/service"
	"net/http"

	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/conf"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/CMSgov/dpc/attribution/router"
	v1 "github.com/CMSgov/dpc/attribution/service/v1"
	v2 "github.com/CMSgov/dpc/attribution/service/v2"
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

	// Create V1 services
	attrDbV1 := repository.GetAttributionV1DbConnection()
	queueDbV1 := repository.GetQueueDbConnection()
	defer func() {
		if err := attrDbV1.Close(); err != nil {
			logger.WithContext(ctx).Fatal("Failed to close attribution v1 db connection", zap.Error(err))
		}
		if err := queueDbV1.Close(); err != nil {
			logger.WithContext(ctx).Fatal("Failed to close queue v1 db connection", zap.Error(err))
		}
	}()
	js, ds := createV1Services(attrDbV1, queueDbV1)

	gr := repository.NewGroupRepo(db)
	gs := v2.NewGroupService(gr, js)

	ir := repository.NewImplementerRepo(db)
	is := v2.NewImplementerService(ir)

	ior := repository.NewImplementerOrgRepo(db)
	autoCreateOrg := conf.GetAsString("autoCreateOrg", "false")

	ios := v2.NewImplementerOrgService(ir, or, ior, autoCreateOrg == "true")

	attributionRouter := router.NewDPCAttributionRouter(os, gs, is, ios, ds, js)

	port := conf.GetAsString("port", "3001")
	fmt.Printf("Starting DPC-Attribution server on port %v ...\n", port)
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), attributionRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}

func createV1Services(attrDbV1 *sql.DB, queueDbV1 *sql.DB) (service.JobService, service.DataService) {
	pr := repository.NewPatientRepo(attrDbV1)
	jr := repository.NewJobRepo(queueDbV1)
	return v1.NewJobService(pr, jr), v1.NewDataService(jr)
}
