package api

import (
	"net/http"

	"github.com/go-chi/chi"

	v2 "github.com/CMSgov/dpc/pkg/api/v2"
)

func NewDPCAPIRouter() http.Handler {
	r := chi.NewRouter()

	r.Route("/api/v2", func(r chi.Router) {
		r.Get("/metadata", v2.Metadata)
		r.Route("/Organization" , func(r chi.Router) {
            r.Route("/{organizationID}", func(r chi.Router) {
                r.Use(v2.OrganizationCtx)
                r.Get("/", v2.NewOrganizationController(nil).GetOrganization)
            })
        })
	})

	return r
}
