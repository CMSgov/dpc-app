package api

import (
	"github.com/go-chi/chi/middleware"
	"net/http"

	"github.com/go-chi/chi"

	v2 "github.com/CMSgov/dpc/api/v2"
)

func NewDPCAPIRouter(oc *v2.OrganizationController) http.Handler {
	r := chi.NewRouter()
	r.With(middleware.RequestID).Route("/v2", func(r chi.Router) {
		r.With(middleware.SetHeader("Content-Type", "application/json+fhir")).Get("/metadata", v2.Metadata)
		r.With(middleware.SetHeader("Content-Type", "application/json+fhir")).Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(v2.OrganizationCtx)
				r.With(FHIRMiddleware).Get("/", oc.GetOrganization)
			})
			r.With(FHIRMiddleware).Post("/", oc.CreateOrganization)
		})
	})

	return r
}
