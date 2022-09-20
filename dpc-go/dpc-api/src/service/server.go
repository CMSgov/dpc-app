package service

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	b64 "encoding/base64"
	"fmt"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/logger"
	"go.uber.org/zap"
	"net/http"
    "time"
)

// Server wrapper struct for an http server.
type Server struct {
	//Server name, used for logs and metadata
	name string

	//Port server will be running on
	port int

	//Determines if type of auth. Supported values: NONE, TLS, MTLS
	authType string

	//Http server
	server http.Server
}

// NewServer creates a new http server wrapper
func NewServer(name string, port int, authType string, handler http.Handler) *Server {
	s := Server{}
	s.name = name
	s.port = port
	s.authType = authType
	s.server = http.Server{
		Handler:      handler,
		Addr:         fmt.Sprintf("%s%d", ":", s.port),
		ReadTimeout:  time.Duration(conf.GetAsInt("SERVER_READ_TIMEOUT_SECONDS", 10)) * time.Second,
		WriteTimeout: time.Duration(conf.GetAsInt("SERVER_WRITE_TIMEOUT_SECONDS", 20)) * time.Second,
		IdleTimeout:  time.Duration(conf.GetAsInt("SERVER_IDLE_TIMEOUT_SECONDS", 120)) * time.Second,
		ReadHeaderTimeout: 2 * time.Second,
	}
	return &s
}

// Serve Start the server, uses Mutual TLS if security is not disabled
func (s *Server) Serve(ctx context.Context) error {
	if s.authType == "NONE" {
		fmt.Printf("Starting UNSECURE %s on port %d\n", s.name, s.port)
		return s.server.ListenAndServe()
	} else if s.authType == "TLS" {
		fmt.Printf("Starting secure TLS %s on port %d\n", s.name, s.port)
		return s.startSecureServer(ctx, false)

	} else if s.authType == "MTLS" {
		fmt.Printf("Starting secure MTLS %s on port %d\n", s.name, s.port)
		return s.startSecureServer(ctx, true)
	}

	return fmt.Errorf("invalid auth type. Supported values: NONE, TLS, MTLS")
}

func (s *Server) startSecureServer(ctx context.Context, useMTLS bool) error {
	caPool, cert := getServerCertificates(ctx)

	severTlSConf := &tls.Config{
		Certificates: []tls.Certificate{cert},
		MinVersion:   tls.VersionTLS12,
	}

	if useMTLS {
		severTlSConf.GetConfigForClient = func(hi *tls.ClientHelloInfo) (*tls.Config, error) {
			serverConf := &tls.Config{
				Certificates: []tls.Certificate{cert},
				MinVersion:   tls.VersionTLS12,
				ClientAuth:   tls.RequireAndVerifyClientCert,
				ClientCAs:    caPool,
			}
			return serverConf, nil
		}
	}

	s.server.TLSConfig = severTlSConf

	if err := s.server.ListenAndServeTLS("", ""); err != nil {
		logger.WithContext(ctx).Fatal(fmt.Sprintf("Failed to start secure server: %s", s.name), zap.Error(err))
	}
	return nil
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
