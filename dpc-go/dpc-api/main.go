package main

import (
	"context"
	"fmt"
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/router"
	"github.com/CMSgov/dpc/api/v2"
	"go.uber.org/zap"
	"net/http"
	"os"
	"strconv"
)

func main() {
	defer logger.Logger.Sync()
	attributionURL, found := os.LookupEnv("ATTRIBUTION_URL")
	if !found {
		attributionURL = "http://localhost:3001"
	}

	retries := os.Getenv("ATTRIBUTION_RETRIES")
	r, err := strconv.Atoi(retries)
	if err != nil {
		r = 3
	}

	attributionClient := client.NewAttributionClient(client.AttributionConfig{
		URL:     attributionURL,
		Retries: r,
	})

	c := v2.NewOrganizationController(attributionClient)

	capabilitiesFile, found := os.LookupEnv("CAPABILITIES_FILE")
	if !found {
		capabilitiesFile = "DPCCapabilities.json"
	}

	m := v2.NewMetadataController(capabilitiesFile)

	apiRouter := router.NewDPCAPIRouter(c, m)

	port := os.Getenv("API_PORT")
	if port == "" {
		port = "3000"
	}
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), apiRouter); err != nil {
		logger.WithContext(context.Background()).Fatal("Failed to start server", zap.Error(err))
	}
}
