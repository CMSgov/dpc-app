package router

import (
	"net/http"

	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/service"
	"github.com/go-chi/chi"
	"github.com/go-chi/chi/middleware"
)

// NewDPCAttributionRouter function to build the attribution router
func NewDPCAttributionRouter(o service.Service, g service.Service, impl service.Service, implOrg service.Service) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware2.RequestIPCtx)
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
			r.Route("/{groupID}", func(r chi.Router) {
				r.Use(middleware2.RequestURLCtx)
				r.Use(middleware2.GroupCtx)
				r.Get("/$export", g.Export)
			})
		})
		r.Route("/Implementer", func(r chi.Router) {
			r.Route("/{implementerID}/org", func(r chi.Router) {
				r.Use(middleware2.ImplementerCtx)
				r.Post("/", implOrg.Post)
				r.Get("/", implOrg.Get)
				r.Delete("/", implOrg.Delete)
				r.Put("/", implOrg.Put)
			})
			r.Post("/", impl.Post)
		})
	})

	return r
}
