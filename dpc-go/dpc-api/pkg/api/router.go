package api

import (
	"net/http"

	"github.com/go-chi/chi"

	"github.com/CMSgov/dpc/auth"
	v2 "github.com/CMSgov/dpc/pkg/api/v2"
)

func NewDPCAPIRouter() http.Handler {
	r := chi.NewRouter()

	r.Route("/api/v2", func(r chi.Router) {
		r.Get("/metadata", v2.Metadata)

		r.Post("/auth/token", auth.GetAuthToken)
		r.Get("/auth/welcome", auth.Welcome)
	})

	return r
}

func NewAuthRouter() http.Handler {
	// return auth.NewAuthRouter(SecurityHeader, ConnectionClose)
	return auth.NewAuthRouter()
}
