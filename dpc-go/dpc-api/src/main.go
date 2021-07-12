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

	ssasClient := client.NewSsasHttpClient(client.SsasHttpClientConfig{
		URL:          conf.GetAsString("ssas-client.url"),
		Retries:      conf.GetAsInt("ssas-client.attrRetries", 3),
		ClientID:     conf.GetAsString("ssas-client.client-id"),
		ClientSecret: conf.GetAsString("ssas-client.client-secret"),
	})
	orgCtrl := v2.NewOrganizationController(attributionClient)
	metaCtrl := v2.NewMetadataController(conf.GetAsString("capabilities.base"))
	groupCtrl := v2.NewGroupController(attributionClient)
	dataCtrl := v2.NewDataController(dataClient)
	jobCtrl := v2.NewJobController(jobClient)
	ssasCtrl := v2.NewSSASController(ssasClient, attributionClient)
	implCtlr := v2.NewImplementerController(attributionClient, ssasClient)
	apiRouter := router.NewDPCAPIRouter(orgCtrl, metaCtrl, groupCtrl, dataCtrl, jobCtrl, ssasCtrl, implCtlr)

	port := conf.GetAsString("port", "3000")
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), apiRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}
