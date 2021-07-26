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

	attrRetries := conf.GetAsInt("attribution-client.retries", 3)

	attributionClient := client.NewAttributionClient(client.AttributionConfig{
		URL:     attributionURL,
		Retries: attrRetries,
	})

	dataClient := client.NewDataClient(client.DataConfig{
		URL:     attributionURL,
		Retries: attrRetries,
	})

	jobClient := client.NewJobClient(client.JobConfig{
		URL:     attributionURL,
		Retries: attrRetries,
	})

	ssasClient := client.NewSsasHTTPClient(client.SsasHTTPClientConfig{
		URL:          conf.GetAsString("ssas-client.url"),
		Retries:      conf.GetAsInt("ssas-client.attrRetries", 3),
		ClientID:     conf.GetAsString("ssas-client.client-id"),
		ClientSecret: conf.GetAsString("ssas-client.client-secret"),
	})

	controllers := router.Controllers{
		Org:      v2.NewOrganizationController(attributionClient),
		Metadata: v2.NewMetadataController(conf.GetAsString("capabilities.base")),
		Group:    v2.NewGroupController(attributionClient),
		Data:     v2.NewDataController(dataClient),
		Job:      v2.NewJobController(jobClient),
		Impl:     v2.NewImplementerController(attributionClient, ssasClient),
		ImplOrg:  v2.NewImplementerOrgController(attributionClient),
		Ssas:     v2.NewSSASController(ssasClient, attributionClient),
	}

	apiRouter := router.NewDPCAPIRouter(controllers)

	port := conf.GetAsString("port", "3000")
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), apiRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}
