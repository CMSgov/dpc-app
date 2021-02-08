package pkg

import (
	"net/http"

	"github.com/go-chi/chi"

	"github.com/CMSgov/dpc/api/pkg/client"
	v2 "github.com/CMSgov/dpc/api/pkg/v2"
)

func NewDPCAPIRouter() http.Handler {
	r := chi.NewRouter()

	c := v2.NewOrganizationController(&client.AttributionConfig{
		URL:     "http://localhost:3001",
		Retries: 3,
	})

	r.Route("/v2", func(r chi.Router) {
		r.Get("/metadata", v2.Metadata)
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(v2.OrganizationCtx)
				r.With(FHIRMiddleware).Get("/", c.GetOrganization)
			})
			r.With(FHIRMiddleware).Post("/", c.CreateOrganization)
		})
	})

	return r
}
