package router

import (
	"net/http"

	"github.com/CMSgov/dpc/api/auth"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	v2 "github.com/CMSgov/dpc/api/v2"
	"github.com/go-chi/chi/middleware"

	"github.com/go-chi/chi"
)

// NewDPCAPIRouter function that builds the router using chi
func NewDPCAPIRouter(oc v2.Controller, mc v2.ReadController) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware.SetHeader("Content-Type", "application/fhir+json; charset=UTF-8"))
	r.Route("/v2", func(r chi.Router) {
		r.Get("/metadata", mc.Read)
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(v2.OrganizationCtx)
				r.With(middleware2.FHIRModel).Get("/", oc.Read)
				r.Delete("/", oc.Delete)
				r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Put("/", oc.Update)
			})
			r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Post("/", oc.Create)
		})
	})
	r.Post("/auth/token", auth.GetAuthToken)
	r.Get("/auth/welcome", auth.Welcome)
	return r
}
