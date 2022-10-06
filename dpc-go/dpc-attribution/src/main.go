package main

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"database/sql"
	b64 "encoding/base64"
	"fmt"
	"github.com/CMSgov/dpc/attribution/client"
	"net/http"

	"github.com/CMSgov/dpc/attribution/conf"
	"github.com/CMSgov/dpc/attribution/logger"
	v1Repo "github.com/CMSgov/dpc/attribution/repository/v1"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/CMSgov/dpc/attribution/router"
	"github.com/CMSgov/dpc/attribution/service"
	v1 "github.com/CMSgov/dpc/attribution/service/v1"
	"time"
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
	js, ds := createJobServices(queueDbV1, or, bfdClient)
	gs := service.NewGroupService(gr, js)

	ir := repository.NewImplementerRepo(db)
	is := service.NewImplementerService(ir)

	ior := repository.NewImplementerOrgRepo(db)
	autoCreateOrg := conf.GetAsString("autoCreateOrg", "false")

	ios := service.NewImplementerOrgService(ir, or, ior, autoCreateOrg == "true")

	attributionRouter := router.NewDPCAttributionRouter(os, gs, is, ios, ds, js)
	port := conf.GetAsString("port", "3001")

	authType := conf.GetAsString("AUTH_TYPE", "TLS")

	if authType == "NONE" {
		startUnsecureServer(ctx, port, attributionRouter)
	} else if authType == "TLS" {
		startTLSServer(ctx, port, attributionRouter)
	} else if authType == "MTLS" {
		startMTLSServer(ctx, port, attributionRouter)
	} else {
		logger.WithContext(ctx).Fatal("Invalid value for DPC_AUTH_TYPE. Supported values: NONE, TLS, MTLS")
	}
}

func startUnsecureServer(ctx context.Context, port string, handler http.Handler) {
	server := http.Server{
		Addr:    fmt.Sprintf(":%s", port),
		Handler: handler,
		ReadHeaderTimeout: 2 * time.Second,
	}
	fmt.Printf("Starting UNSECURE DPC-Attribution server on port %v ...\n", port)
	if err := server.ListenAndServe(); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}

func startTLSServer(ctx context.Context, port string, handler http.Handler) {
	fmt.Printf("Starting secure TLS DPC-Attribution server on port %v ...\n", port)
	startSecureServer(ctx, port, handler, false)
}

func startMTLSServer(ctx context.Context, port string, handler http.Handler) {
	fmt.Printf("Starting secure MTLS DPC-Attribution server on port %v ...\n", port)
	startSecureServer(ctx, port, handler, true)
}

func startSecureServer(ctx context.Context, port string, handler http.Handler, useMTLS bool) {
	caPool, cert := getServerCertificates(ctx)

	severConf := &tls.Config{
		Certificates: []tls.Certificate{cert},
		MinVersion:   tls.VersionTLS12,
		GetConfigForClient: func(hi *tls.ClientHelloInfo) (*tls.Config, error) {
			serverConf := &tls.Config{
				Certificates: []tls.Certificate{cert},
				MinVersion:   tls.VersionTLS12,
				ClientCAs:    caPool,
			}

			if useMTLS {
				serverConf.ClientAuth = tls.RequireAndVerifyClientCert
			}
			return serverConf, nil
		},
	}

	server := http.Server{
		Addr:      fmt.Sprintf(":%s", port),
		Handler:   handler,
		TLSConfig: severConf,
		ReadHeaderTimeout: 2 * time.Second,
	}
	//If cert and key file paths are not passed the certs in tls configs are used.
	if err := server.ListenAndServeTLS("", ""); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}

func createJobServices(queueDbV1 *sql.DB, or repository.OrganizationRepo, client client.APIClient) (v1.JobService, v1.DataService) {
	jr := v1Repo.NewJobRepo(queueDbV1)
	return v1.NewJobService(jr, or, client), v1.NewDataService(jr)
}

func getServerCertificates(ctx context.Context) (*x509.CertPool, tls.Certificate) {
	crtB, err := b64.StdEncoding.DecodeString(conf.GetAsString("CERT"))
	if err != nil {
		logger.WithContext(ctx).Fatal("Could not base64 decode DPC_CERT", zap.Error(err))
	}
	keyB, err := b64.StdEncoding.DecodeString(conf.GetAsString("CERT_KEY"))
	if err != nil {
		logger.WithContext(ctx).Fatal("Could not base64 decode DPC_CERT_KEY", zap.Error(err))
	}

	crtStr := string(crtB)
	keyStr := string(keyB)

	if crtStr == "" || keyStr == "" {
		logger.WithContext(ctx).Fatal("One of the following required environment variables is missing or not base64 encoded: DPC_CA_CERT, DPC_CERT, DPC_CERT_KEY")
	}

	// We are using the server cert as the CA cert.
	certPool := x509.NewCertPool()
	ok := certPool.AppendCertsFromPEM([]byte(crtStr))
	if !ok {
		logger.WithContext(ctx).Fatal("Failed to parse server cert")
	}

	crt, err := tls.X509KeyPair([]byte(crtStr), []byte(keyStr))
	if err != nil {
		logger.WithContext(ctx).Fatal("Failed to parse server cert/key par", zap.Error(err))
	}
	return certPool, crt
}
