
package service

import (
	"context"
	"fmt"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/logger"
	"net/http"
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
	//TODO MTLS logic will go here once it is merged in.
	logger.WithContext(ctx).Fatal("Secure server not yet implemented")
	return nil
}
