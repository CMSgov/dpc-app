package main

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/router"
	v2 "github.com/CMSgov/dpc/api/v2"
	"go.uber.org/zap"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"strings"
	"time"
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

	createCertFiles()

	caCertFile, err := ioutil.ReadFile("../cert/ca.crt")
	if err != nil {
		logger.WithContext(ctx).Fatal("error reading CA certificate", zap.Error(err))
	}
	certPool := x509.NewCertPool()
	certPool.AppendCertsFromPEM(caCertFile)

	cer, err := tls.LoadX509KeyPair("../cert/server.crt", "../cert/server.key")
	if err != nil {
		log.Println(err)
		return
	}

	severConf := &tls.Config{
		GetConfigForClient: func(hi *tls.ClientHelloInfo) (*tls.Config, error) {
			serverConf := &tls.Config{
				Certificates:          []tls.Certificate{cer},
				MinVersion:            tls.VersionTLS12,
				ClientAuth:            tls.RequireAndVerifyClientCert,
				ClientCAs:             certPool,
				VerifyPeerCertificate: getClientValidator(hi, certPool),
			}
			return serverConf, nil
		},
	}

	port := conf.GetAsString("port", "3000")
	server := http.Server{
		Addr:      fmt.Sprintf(":%s", port),
		Handler:   apiRouter,
		TLSConfig: severConf,
	}

	if err := server.ListenAndServeTLS("../cert/server.crt", "../cert/server.key"); err != nil {
		logger.WithContext(ctx).Fatal("Failed to start server", zap.Error(err))
	}
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

func createCertFiles() {
	caCrt := conf.GetAsString("ca-cert")
	servCrt := conf.GetAsString("server-cert")
	servKey := conf.GetAsString("server-key")

	saveCert(caCrt, "../cert/ca.crt")
	saveCert(servCrt, "../cert/server.crt")
	saveCert(servKey, "../cert/server.key")
}

func saveCert(input string, dest string) {
	f, err := os.Create(dest)

	if err != nil {
		log.Fatal(err)
	}
	defer f.Close()

	nl := strings.ReplaceAll(input, "\\n", "\n")
	_, err = f.WriteString(nl)

	if err != nil {
		log.Fatal(err)
	}
}
