package api

import (
	"github.com/CMSgov/dpc/api/v2"
	"github.com/go-chi/chi/middleware"
	"net/http"

	"github.com/go-chi/chi"
)

func NewDPCAPIRouter(oc *v2.OrganizationController, mc *v2.MetadataController) http.Handler {
	r := chi.NewRouter()
	r.With(middleware.RequestID).Route("/v2", func(r chi.Router) {
		r.With(FHIRContentType).Get("/metadata", mc.Get)
		r.With(FHIRContentType).Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(v2.OrganizationCtx)
				r.With(FHIRModel).Get("/", oc.Get)
			})
			r.With(FHIRModel).Post("/", oc.Create)
		})
	})

	return r
}
