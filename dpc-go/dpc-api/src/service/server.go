package service

import (
    "fmt"
    "github.com/CMSgov/dpc/api/logger"
    "net/http"
    "time"
    "context"
)

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

func NewServer(name string, port int, securityDisabled bool, handler http.Handler) *Server {
	s := Server{}
	s.name = name
	s.port = port
	s.securityDisabled = securityDisabled
	s.server = http.Server{
		Handler: handler,
		Addr:    fmt.Sprintf("%s%d", ":", s.port),
		//TODO make these configurable
		ReadTimeout:  time.Duration(10) * time.Second,
		WriteTimeout: time.Duration(20) * time.Second,
		IdleTimeout:  time.Duration(120) * time.Second,
	}
	return &s
}

func (s *Server) Serve(ctx context.Context) error{
    if s.securityDisabled {
        fmt.Printf("Starting UNSECURE %s on port %d\n", s.name, s.port)
        return s.server.ListenAndServe()
    }else{
        //TODO MTLS logic will go here once it is merged in.
        logger.WithContext(ctx).Fatal("Secure server not yet implemented")
    }
    return nil
}
