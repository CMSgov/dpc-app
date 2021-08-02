package main

import (
	"context"
	"database/sql"
	"fmt"
	"github.com/CMSgov/dpc/attribution/client"
	"github.com/CMSgov/dpc/attribution/service"
	"net/http"

	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/conf"
	"github.com/CMSgov/dpc/attribution/logger"
	v1Repo "github.com/CMSgov/dpc/attribution/repository/v1"

	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/CMSgov/dpc/attribution/router"
	v1 "github.com/CMSgov/dpc/attribution/service/v1"
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
	os := service.NewOrganizationService(or)

	// Create V1 services
	queueDbV1 := v1Repo.GetQueueDbConnection()
	defer func() {
		if err := queueDbV1.Close(); err != nil {
			logger.WithContext(ctx).Fatal("Failed to close queue v1 db connection", zap.Error(err))
		}
	}()

	bfdClient, err := client.NewBfdClient(client.NewConfig("/v2/fhir/"))
	if err != nil {
		logger.WithContext(ctx).Fatal("Failed to create BFD client", zap.Error(err))
	}

	gr := repository.NewGroupRepo(db)
	js, ds := createJobServices(queueDbV1, or, gr, bfdClient)
	gs := service.NewGroupService(gr, js)

	ir := repository.NewImplementerRepo(db)
	is := service.NewImplementerService(ir)

	ior := repository.NewImplementerOrgRepo(db)
	autoCreateOrg := conf.GetAsString("autoCreateOrg", "false")

	ios := service.NewImplementerOrgService(ir, or, ior, autoCreateOrg == "true")

	attributionRouter := router.NewDPCAttributionRouter(os, gs, is, ios, ds, js)

	port := conf.GetAsString("port", "3001")
	fmt.Printf("Starting DPC-Attribution server on port %v ...\n", port)
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), attributionRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}

func createJobServices(queueDbV1 *sql.DB, or repository.OrganizationRepo, gr repository.GroupRepo, client client.APIClient) (v1.JobService, v1.DataService) {
	jr := v1Repo.NewJobRepo(queueDbV1)
	return v1.NewJobService(jr, or, gr, client), v1.NewDataService(jr)
}
