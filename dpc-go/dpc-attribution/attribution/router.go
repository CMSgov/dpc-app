package attribution

import (
	"github.com/go-chi/chi"
	"github.com/go-chi/chi/middleware"
	"net/http"

	v2 "github.com/CMSgov/dpc/attribution/v2"
)

func NewDPCAttributionRouter() http.Handler {
	r := chi.NewRouter()
	c := v2.NewOrganizationController()
	r.With(middleware.RequestID).Route("/", func(r chi.Router) {
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(v2.OrganizationCtx)
				r.Get("/", c.GetOrganization)
			})
			r.Post("/", c.SaveOrganization)
		})
	})

	return r
}
