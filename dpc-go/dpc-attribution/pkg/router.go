package pkg

import (
	"github.com/go-chi/chi"
	"net/http"

	v2 "github.com/CMSgov/dpc/attribution/pkg/v2"
)

func NewDPCAttributionRouter() http.Handler {
	r := chi.NewRouter()
	c := v2.NewOrganizationController()
	r.Route("/", func(r chi.Router) {
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(v2.OrganizationCtx)
				r.With(FHIRMiddleware).Get("/", c.GetOrganization)
			})
			r.With(FHIRMiddleware).Post("/", c.SaveOrganization)
		})
	})

	return r
}
