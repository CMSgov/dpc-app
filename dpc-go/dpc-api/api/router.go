package api

import (
	"github.com/CMSgov/dpc/api/v2"
	"github.com/go-chi/chi/middleware"
	"net/http"

	"github.com/go-chi/chi"
)

func NewDPCAPIRouter(oc v2.Controller, mc v2.ReadController) http.Handler {
	r := chi.NewRouter()
	r.With(middleware.RequestID, middleware.SetHeader("Content-Type", "application/fhir+json; charset=UTF-8")).Route("/v2", func(r chi.Router) {
		r.Get("/metadata", mc.Read)
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(v2.OrganizationCtx)
				r.With(FHIRModel).Get("/", oc.Read)
			})
			r.With(FHIRModel).Post("/", oc.Create)
		})
	})

	return r
}
