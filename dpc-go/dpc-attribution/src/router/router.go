package router

import (
	"net/http"

	"github.com/go-chi/chi"
	"github.com/go-chi/chi/middleware"

	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/service"
)

// NewDPCAttributionRouter function to build the attribution router
func NewDPCAttributionRouter(o service.Service, g service.Service) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware.SetHeader("Content-Type", "application/json; charset=UTF-8"))
	r.Use(middleware2.AuthCtx)
	r.Use(middleware2.RequestIPCtx)
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
            r.Use(middleware2.GroupCtx)
			r.Post("/", g.Post)
            r.Get("/$export", g.Export)
		})
	})

	return r
}
