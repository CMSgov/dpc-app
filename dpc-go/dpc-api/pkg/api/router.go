package api

import (
    "net/http"

    "github.com/go-chi/chi"

    "github.com/CMSgov/dpc/pkg/api/client"
    v2 "github.com/CMSgov/dpc/pkg/api/v2"
)

func NewDPCAPIRouter() http.Handler {
	r := chi.NewRouter()

	r.Route("/api/v2", func(r chi.Router) {
		r.Get("/metadata", v2.Metadata)
		r.Route("/Organization" , func(r chi.Router) {
            r.Route("/{organizationID}", func(r chi.Router) {
                r.Use(v2.OrganizationCtx)
                r.Get("/", v2.NewOrganizationController(&client.AttributionConfig{
                    URL: "http://localhost:3001/attribution",
                    Retries: 3,
                }).GetOrganization)
            })
        })
	})

	return r
}
