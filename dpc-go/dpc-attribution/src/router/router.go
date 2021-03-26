package router

import (
	"github.com/go-chi/chi"
	"github.com/go-chi/chi/middleware"
	"net/http"

	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	v2 "github.com/CMSgov/dpc/attribution/v2"
)

// NewDPCAttributionRouter function to build the attribution router
func NewDPCAttributionRouter(o v2.Service, g *v2.GroupService) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware.SetHeader("Content-Type", "application/json; charset=UTF-8"))
	r.Use(middleware2.OrgHeader)
	r.Route("/", func(r chi.Router) {
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(v2.OrganizationCtx)
				r.Get("/", o.Get)
				r.Delete("/", o.Delete)
				r.Put("/", o.Put)
			})
			r.Post("/", o.Post)
		})
		r.Route("/Group", func(r chi.Router) {
			r.Use(v2.GroupCtx)
			r.Get("/{groupID}", g.Get)
			r.Get("/$export", g.Export)
		})
	})

	return r
}
