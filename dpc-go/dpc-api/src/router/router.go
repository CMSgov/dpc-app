package router

import (
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/v2"
	"github.com/go-chi/chi/middleware"
	"net/http"

	"github.com/go-chi/chi"
)

// NewDPCAPIRouter function that builds the router using chi
func NewDPCAPIRouter(mc v2.ReadController, oc v2.Controller, pc v2.Controller) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware.SetHeader("Content-Type", "application/fhir+json; charset=UTF-8"))
	r.Use(middleware2.OrgHeader)
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
		r.Route("/Practitioner", func(r chi.Router) {
			r.Route("/{practitionerID}", func(r chi.Router) {
				r.Use(v2.PractitionerCtx)
				r.With(middleware2.FHIRModel).Get("/", pc.Read)
			})
			r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Post("/", pc.Create)
		})
	})

	return r
}
