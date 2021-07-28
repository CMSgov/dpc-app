package admin

import (
	"context"
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/conf"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/service"
	"github.com/go-chi/chi"

	v2 "github.com/CMSgov/dpc/api/v2"
	"github.com/go-chi/chi/middleware"
	"net/http"
)

func buildAdminRoutes(c controllers) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware2.RequestIPCtx)
	r.With(middleware2.Sanitize).Route("/v2", func(r chi.Router) {
		r.Use(middleware.SetHeader("Content-Type", "application/fhir+json; charset=UTF-8"))

		//ORGANIZATION Routes
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(middleware2.OrganizationCtx)
				r.With(middleware2.FHIRModel).Get("/", c.Org.Read)
				r.Delete("/", c.Org.Delete)
				r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Put("/", c.Org.Update)
			})
			r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Post("/", c.Org.Create)
		})

		//IMPLEMENTER Routes
		r.Route("/Implementer", func(r chi.Router) {
			r.Post("/", c.Impl.Create)
			r.Route("/{implementerID}/org", func(r chi.Router) {
				r.Use(middleware2.ImplementerCtx)
				r.Get("/", c.ImplOrg.Read)
				r.Post("/", c.ImplOrg.Create)
			})
		})
		//IMPLEMENTER ORG
		r.Route("/Implementer/{implementerID}/Org/{organizationID}/system", func(r chi.Router) {
			r.With(middleware2.ImplementerCtx).With(middleware2.OrganizationCtx).Post("/", c.Ssas.CreateSystem)
			r.With(middleware2.ImplementerCtx, middleware2.OrganizationCtx).Get("/", c.Ssas.GetSystem)
		})
	})
	return r
}

// NewAdminServer configures clients, builds ADMIN routes, and creates a server.
func NewAdminServer(ctx context.Context) *service.Server {
	attrClient := client.NewAttributionClient(client.AttributionConfig{
		URL:     conf.GetAsString("attribution-client.url"),
		Retries: conf.GetAsInt("attribution-client.retries", 3),
	})

	ssasClient := client.NewSsasHTTPClient(client.SsasHTTPClientConfig{
		PublicURL:    conf.GetAsString("ssas-client.public-url"),
		AdminURL:     conf.GetAsString("ssas-client.admin-url"),
		Retries:      conf.GetAsInt("ssas-client.attrRetries", 3),
		ClientID:     conf.GetAsString("ssas-client.client-id"),
		ClientSecret: conf.GetAsString("ssas-client.client-secret"),
	})

	port := conf.GetAsInt("ADMIN_PORT", 3011)

	controllers := controllers{
		Org:     v2.NewOrganizationController(attrClient),
		Impl:    v2.NewImplementerController(attrClient, ssasClient),
		ImplOrg: v2.NewImplementerOrgController(attrClient),
		Ssas:    v2.NewSSASController(ssasClient, attrClient),
	}

	r := buildAdminRoutes(controllers)
	return service.NewServer("DPC-API Admin Server", port, true, r)
}

type controllers struct {
	Org     v2.Controller
	Impl    v2.Controller
	ImplOrg v2.Controller
	Ssas    v2.SsasController
}
