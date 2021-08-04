package main

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"database/sql"
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
	"strings"
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

	authDisabled := conf.GetAsString("AUTH_DISABLED", "false")

	if authDisabled == "true" {
		startUnsecureServer(ctx, port, attributionRouter)
	} else {
		startSecureServer(ctx, port, attributionRouter)
	}
}

func startUnsecureServer(ctx context.Context, port string, handler http.Handler) {
	server := http.Server{
		Addr:    fmt.Sprintf(":%s", port),
		Handler: handler,
	}
	fmt.Printf("Starting UNSECURE DPC-Attribution server on port %v ...\n", port)
	if err := server.ListenAndServe(); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}

func startSecureServer(ctx context.Context, port string, handler http.Handler) {
	caPool, cert := getServerCertificates(ctx)

	severConf := &tls.Config{
		Certificates: []tls.Certificate{cert},
		MinVersion:   tls.VersionTLS12,
		GetConfigForClient: func(hi *tls.ClientHelloInfo) (*tls.Config, error) {
			serverConf := &tls.Config{
				Certificates:          []tls.Certificate{cert},
				MinVersion:            tls.VersionTLS12,
				ClientAuth:            tls.RequireAndVerifyClientCert,
				ClientCAs:             caPool,
				VerifyPeerCertificate: getClientValidator(hi, caPool),
			}
			return serverConf, nil
		},
	}

	server := http.Server{
		Addr:      fmt.Sprintf(":%s", port),
		Handler:   handler,
		TLSConfig: severConf,
	}

	fmt.Printf("Starting DPC-Attribution server on port %v ...\n", port)
	//If cert and key file paths are not passed the certs in tls configs are used.
	if err := server.ListenAndServeTLS("", ""); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
}

func createJobServices(queueDbV1 *sql.DB, or repository.OrganizationRepo, client client.APIClient) (v1.JobService, v1.DataService) {
	jr := v1Repo.NewJobRepo(queueDbV1)
	return v1.NewJobService(jr, or, client), v1.NewDataService(jr)
}

func getClientValidator(helloInfo *tls.ClientHelloInfo, cerPool *x509.CertPool) func([][]byte, [][]*x509.Certificate) error {
	acS := conf.GetAsString("ALLOWED_CLIENTS", "local.api.dpc.cms.gov")
	allowed := strings.Split(acS, ",")
	return func(rawCerts [][]byte, verifiedChains [][]*x509.Certificate) error {
		for _, dnsN := range verifiedChains[0][0].DNSNames {
			for _, n := range allowed {
				if n == dnsN {
					return nil
				}
			}
		}
		return fmt.Errorf("client's SAN does not contain one of the allowed alt name. Allowed: %s", allowed)
	}
}

func getServerCertificates(ctx context.Context) (*x509.CertPool, tls.Certificate) {
	caStr := strings.ReplaceAll(conf.GetAsString("CA_CERT"), "\\n", "\n")
	crtStr := strings.ReplaceAll(conf.GetAsString("CERT"), "\\n", "\n")
	keyStr := strings.ReplaceAll(conf.GetAsString("CERT_KEY"), "\\n", "\n")

	if caStr == "" || crtStr == "" || keyStr == "" {
		logger.WithContext(ctx).Fatal("One of the following required environment variables is missing: DPC_CA_CERT, DPC_SERVER_CERT, DPC_SERVER_CERT_KEY")
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
