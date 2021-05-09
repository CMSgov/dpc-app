package router

import (
	"github.com/go-chi/chi"
	"github.com/go-chi/chi/middleware"
	"net/http"

	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	v2 "github.com/CMSgov/dpc/attribution/v2"
)

// NewDPCAttributionRouter function to build the attribution router
func NewDPCAttributionRouter(o v2.Service, g v2.PostService, impl v2.PostService) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware.SetHeader("Content-Type", "application/json; charset=UTF-8"))
	r.Route("/", func(r chi.Router) {
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(middleware2.OrganizationCtx)
				r.Get("/", o.Get)
				r.Delete("/", o.Delete)
				r.Put("/", o.Put)
			})
			r.Post("/", o.Post)
		})
		r.Route("/Group", func(r chi.Router) {
			r.Use(middleware2.AuthCtx)
			r.Post("/", g.Post)
		})
		r.Route("/Implementer", func(r chi.Router) {
			r.Post("/", impl.Post)
		})
	})

	return r
}
