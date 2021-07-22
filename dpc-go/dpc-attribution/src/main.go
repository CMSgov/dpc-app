package main

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"database/sql"
	"fmt"
	"github.com/CMSgov/dpc/attribution/service"
	"net/http"
	"strings"
	"time"

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

	defer func() {
		if err := db.Close(); err != nil {
			logger.WithContext(ctx).Fatal("Failed to close db connection", zap.Error(err))
		}
	}()

	or := repository.NewOrganizationRepo(db)
	os := v2.NewOrganizationService(or)

	// Create V1 services
	attrDbV1 := repository.GetAttributionV1DbConnection()
	queueDbV1 := repository.GetQueueDbConnection()
	defer func() {
		if err := attrDbV1.Close(); err != nil {
			logger.WithContext(ctx).Fatal("Failed to close attribution v1 db connection", zap.Error(err))
		}
		if err := queueDbV1.Close(); err != nil {
			logger.WithContext(ctx).Fatal("Failed to close queue v1 db connection", zap.Error(err))
		}
	}()
	js, ds := createV1Services(attrDbV1, queueDbV1)

	gr := repository.NewGroupRepo(db)
	gs := v2.NewGroupService(gr, js)

	ir := repository.NewImplementerRepo(db)
	is := v2.NewImplementerService(ir)

	ior := repository.NewImplementerOrgRepo(db)
	autoCreateOrg := conf.GetAsString("autoCreateOrg", "false")

	ios := v2.NewImplementerOrgService(ir, or, ior, autoCreateOrg == "true")

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

func createV1Services(attrDbV1 *sql.DB, queueDbV1 *sql.DB) (service.JobService, service.DataService) {
	pr := repository.NewPatientRepo(attrDbV1)
	jr := repository.NewJobRepo(queueDbV1)
	return v1.NewJobService(pr, jr), v1.NewDataService(jr)
}

func getClientValidator(helloInfo *tls.ClientHelloInfo, cerPool *x509.CertPool) func([][]byte, [][]*x509.Certificate) error {
	return func(rawCerts [][]byte, verifiedChains [][]*x509.Certificate) error {
		//Taken from src/crypto/tls/handshake_server.go
		rAdd := helloInfo.Conn.RemoteAddr().String()
		host := rAdd[:strings.LastIndex(rAdd, ":")]
		opts := x509.VerifyOptions{
			Roots:         cerPool,
			CurrentTime:   time.Now(),
			Intermediates: x509.NewCertPool(),
			KeyUsages:     []x509.ExtKeyUsage{x509.ExtKeyUsageClientAuth},
			DNSName:       host,
		}
		_, err := verifiedChains[0][0].Verify(opts)
		return err
	}
}

func getServerCertificates(ctx context.Context) (*x509.CertPool, tls.Certificate) {
	caStr := strings.ReplaceAll(conf.GetAsString("CA_CERT"), "\\n", "\n")
	crtStr := strings.ReplaceAll(conf.GetAsString("SERVER_CERT"), "\\n", "\n")
	keyStr := strings.ReplaceAll(conf.GetAsString("SERVER_CERT_KEY"), "\\n", "\n")

	if caStr == "" {
		logger.WithContext(ctx).Fatal("Missing server ca cert, env variable CA_CERT is required")
	}

	if crtStr == "" {
		logger.WithContext(ctx).Fatal("Missing server cert, env variable SERVER_CERT is required")
	}

	if keyStr == "" {
		logger.WithContext(ctx).Fatal("Missing server cert key, env variable SERVER_CERT_KEY is required")
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
