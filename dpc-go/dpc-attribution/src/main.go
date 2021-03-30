package main

import (
	"context"
	"fmt"
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
	attrDbV1 := repository.GetAttributionV1DbConnection()
	queueDbV1 := repository.GetQueueDbConnection()
	defer func() {
		if err := db.Close(); err != nil {
			logger.WithContext(ctx).Fatal("Failed to close db connection", zap.Error(err))
		}
		if err := attrDbV1.Close(); err != nil {
			logger.WithContext(ctx).Fatal("Failed to close attribution v1 db connection", zap.Error(err))
		}
		if err := queueDbV1.Close(); err != nil {
			logger.WithContext(ctx).Fatal("Failed to close queue v1 db connection", zap.Error(err))
		}
	}()

	or := repository.NewOrganizationRepo(db)
	os := v2.NewOrganizationService(or)

	pr := repository.NewPatientRepo(attrDbV1)
	jr := repository.NewJobRepo(queueDbV1)
	js := v1.NewJobServiceV1(pr, jr)
	gs := v2.NewGroupService(js)

	attributionRouter := router.NewDPCAttributionRouter(os, gs)

	port := conf.GetAsString("port", "3001")
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), attributionRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}
