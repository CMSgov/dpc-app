package router

import (
	"github.com/go-chi/chi"
	"github.com/go-chi/chi/middleware"
	"net/http"

	v2 "github.com/CMSgov/dpc/attribution/v2"
)

func NewDPCAttributionRouter(o v2.Service) http.Handler {
	r := chi.NewRouter()
	r.With(middleware.RequestID, middleware.SetHeader("Content-Type", "application/json; charset=UTF-8")).Route("/", func(r chi.Router) {
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(v2.OrganizationCtx)
				r.Get("/", o.Get)
				r.Delete("/", o.Delete)
				r.Put("/", o.Put)
			})
			r.Post("/", o.Post)
		})
	})

	return r
}
