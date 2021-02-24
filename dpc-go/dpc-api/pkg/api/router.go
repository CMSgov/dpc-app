package api

import (
	"net/http"

	"github.com/go-chi/chi"

	auth "github.com/CMSgov/dpc/auth"
	v2 "github.com/CMSgov/dpc/pkg/api/v2"
)

func NewDPCAPIRouter() http.Handler {
	r := chi.NewRouter()

	r.Route("/api/v2", func(r chi.Router) {
		r.Get("/metadata", v2.Metadata)
	})

	return r
}

func NewAuthRouter() http.Handler {
	return auth.NewAuthRouter(SecurityHeader, ConnectionClose)
}
