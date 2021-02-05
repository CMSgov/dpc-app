package attribution

import (
	"net/http"

	"github.com/go-chi/chi"

	v2 "github.com/CMSgov/dpc/pkg/attribution/v2"
)

func NewDPCAttributionRouter() http.Handler {
	r := chi.NewRouter()

	r.Route("/attribution", func(r chi.Router) {
		r.Route("/Organization" , func(r chi.Router) {
            r.Route("/{organizationID}", func(r chi.Router) {
                r.Use(v2.OrganizationCtx)
                r.Get("/", v2.NewOrganizationController().GetOrganization)
            })
        })
	})

	return r
}
