package service

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/logger"
	"go.uber.org/zap"
	"net/http"
	"strings"
	"time"
)

// Server wrapper struct for an http server.
type Server struct {
	//Server name, used for logs and metadata
	name string

	//Port server will be running on
	port int

	//Determines if MTLS will be disabled
	securityDisabled bool

	//Http server
	server http.Server
}

// NewServer creates a new http server wrapper
func NewServer(name string, port int, securityDisabled bool, handler http.Handler) *Server {
	s := Server{}
	s.name = name
	s.port = port
	s.securityDisabled = securityDisabled
	s.server = http.Server{
		Handler:      handler,
		Addr:         fmt.Sprintf("%s%d", ":", s.port),
		ReadTimeout:  time.Duration(conf.GetAsInt("SERVER_READ_TIMEOUT_SECONDS", 10)) * time.Second,
		WriteTimeout: time.Duration(conf.GetAsInt("SERVER_WRITE_TIMEOUT_SECONDS", 20)) * time.Second,
		IdleTimeout:  time.Duration(conf.GetAsInt("SERVER_IDLE_TIMEOUT_SECONDS", 120)) * time.Second,
	}
	return &s
}

// Serve Start the server, uses Mutual TLS if security is not disabled
func (s *Server) Serve(ctx context.Context) error {
	if s.securityDisabled {
		fmt.Printf("Starting UNSECURE %s on port %d\n", s.name, s.port)
		return s.server.ListenAndServe()
	}

	caPool, cert := getServerCertificates(ctx)

	s.server.TLSConfig = &tls.Config{
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

	if err := s.server.ListenAndServeTLS("", ""); err != nil {
		logger.WithContext(ctx).Fatal(fmt.Sprintf("Failed to start secure server: %s", s.name), zap.Error(err))
	}
	return nil
}

func getClientValidator(helloInfo *tls.ClientHelloInfo, cerPool *x509.CertPool) func([][]byte, [][]*x509.Certificate) error {
	reqName := conf.GetAsString("TLS_REQUIRED_ALT_NAME", "api.user.dpc.cms.gov")
	return func(rawCerts [][]byte, verifiedChains [][]*x509.Certificate) error {
		for _, n := range verifiedChains[0][0].DNSNames {
			if n == reqName {
				return nil
			}
		}
		return fmt.Errorf("client's SAN does not contain required name: %s", reqName)
	}
}

func getServerCertificates(ctx context.Context) (*x509.CertPool, tls.Certificate) {
	caStr := strings.ReplaceAll(conf.GetAsString("CA_CERT"), "\\n", "\n")
	crtStr := strings.ReplaceAll(conf.GetAsString("SERVER_CERT"), "\\n", "\n")
	keyStr := strings.ReplaceAll(conf.GetAsString("SERVER_CERT_KEY"), "\\n", "\n")

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
