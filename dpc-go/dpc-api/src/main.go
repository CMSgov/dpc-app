package main

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"net/http"
	"strings"

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

	caPool, crt := getAttrCertificates(ctx)

	attributionClient := client.NewAttributionClient(client.AttributionConfig{
		URL:        attributionURL,
		Retries:    attrRetries,
		CACertPool: caPool,
		Cert:       crt,
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
		PublicURL:    conf.GetAsString("ssas-client.public-url"),
		AdminURL:     conf.GetAsString("ssas-client.admin-url"),
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
	fmt.Printf("Starting DPC-API server on port %v ...\n", port)
	if err := http.ListenAndServe(fmt.Sprintf(":%s", port), apiRouter); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}

func getAttrCertificates(ctx context.Context) (*x509.CertPool, tls.Certificate) {
	caStr := strings.ReplaceAll(conf.GetAsString("ATTRIBUTION_CLIENT_CA_CERT"), "\\n", "\n")
	crtStr := strings.ReplaceAll(conf.GetAsString("ATTRIBUTION_CLIENT_CERT"), "\\n", "\n")
	keyStr := strings.ReplaceAll(conf.GetAsString("ATTRIBUTION_CLIENT_CERT_KEY"), "\\n", "\n")

	if caStr == "" {
		logger.WithContext(ctx).Warn("Missing server ca cert, env variable ATTRIBUTION_CLIENT_CA_CERT is required when using MTLS")
	}

	if crtStr == "" {
		logger.WithContext(ctx).Warn("Missing server cert, env variable ATTRIBUTION_CLIENT_CERT is required when using MTLS")
	}

	if keyStr == "" {
		logger.WithContext(ctx).Warn("Missing server cert key, env variable ATTRIBUTION_CLIENT_CERT_KEY is required when using MTLS")
	}

	if caStr == "" || crtStr == "" || keyStr == "" {
		return nil, tls.Certificate{}
	}

	certPool := x509.NewCertPool()
	ok := certPool.AppendCertsFromPEM([]byte(caStr))
	if !ok {
		logger.WithContext(ctx).Fatal("Failed to parse server CA cert")
	}

	crt, err := tls.X509KeyPair([]byte(crtStr), []byte(keyStr))
	if err != nil {
		logger.WithContext(ctx).Fatal("Failed to parse server cert/key par", zap.Error(err))
	}

	return certPool, crt
}
