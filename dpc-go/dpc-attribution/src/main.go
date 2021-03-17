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

	r := repository.NewOrganizationRepo(db)
	pr := repository.NewPractitionerRepo(db)
	c := v2.NewOrganizationService(r)
	ps := v2.NewPractitionerService(pr)

	attributionRouter := router.NewDPCAttributionRouter(c, ps)

	port := conf.GetAsString("port", "3001")
	fmt.Println("Attribution running on port", port)
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), attributionRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}
