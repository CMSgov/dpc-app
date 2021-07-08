package main

import (
	"context"
	"fmt"
	"net/http"

	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/router"
	v2 "github.com/CMSgov/dpc/api/v2"
	"go.uber.org/zap"
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

	dataClient := client.NewDataClient(client.DataConfig{
		URL:     attributionURL,
		Retries: retries,
	})

	capabilitiesFile := conf.GetAsString("capabilities.base")

	jobClient := client.NewJobClient(client.JobConfig{
		URL:     attributionURL,
		Retries: retries,
	})

	controllers := router.Controllers{
		Org:      v2.NewOrganizationController(attributionClient),
		Metadata: v2.NewMetadataController(capabilitiesFile),
		Group:    v2.NewGroupController(attributionClient),
		Data:     v2.NewDataController(dataClient),
		Job:      v2.NewJobController(jobClient),
		Impl:     v2.NewImplementerController(attributionClient),
		ImplOrg:  v2.NewImplementerOrgController(attributionClient),
	}

	apiRouter := router.NewDPCAPIRouter(controllers)

	port := conf.GetAsString("port", "3000")
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), apiRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}
