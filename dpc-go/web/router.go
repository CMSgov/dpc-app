package web

import (
    "net/http"

	"github.com/go-chi/chi"

	v2 "github.com/CMSgov/dpc/api/v2"
)

func NewDPCAPIRouter() http.Handler {
	r := chi.NewRouter()

	r.Route("/api/v2", func(r chi.Router) {
		r.Get("/metadata", v2.Metadata)
	})

	return r
}
