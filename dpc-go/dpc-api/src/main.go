package main

import (
	"context"
	"fmt"
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/router"
	"github.com/CMSgov/dpc/api/v2"
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
	attributionURL := conf.GetAsString("attribution-client.url")

	retries := conf.GetAsInt("attribution-client.retries", 3)

	attributionClient := client.NewAttributionClient(client.AttributionConfig{
		URL:     attributionURL,
		Retries: retries,
	})

	oc := v2.NewOrganizationController(attributionClient)
	pc := v2.NewPractitionerController(attributionClient)

	capabilitiesFile := conf.GetAsString("capabilities.base")

	m := v2.NewMetadataController(capabilitiesFile)

	apiRouter := router.NewDPCAPIRouter(m, oc, pc)

	port := conf.GetAsString("port", "3000")
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), apiRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}
