package admin

import (
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/conf"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/service"
	"github.com/go-chi/chi"

	v2 "github.com/CMSgov/dpc/api/v2"
	"github.com/go-chi/chi/middleware"
	"net/http"
)

func buildAdminRoutes(oc v2.Controller, ic v2.Controller, ioc v2.Controller, sc v2.SsasController) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware2.RequestIPCtx)
	r.With(middleware2.Sanitize).Route("/v2", func(r chi.Router) {
		r.Use(middleware.SetHeader("Content-Type", "application/fhir+json; charset=UTF-8"))

		//ORGANIZATION Routes
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(middleware2.OrganizationCtx)
				//TODO Shouldn't org/:id be public?
				r.With(middleware2.FHIRModel).Get("/", oc.Read)
				r.Delete("/", oc.Delete)
				r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Put("/", oc.Update)
			})
			r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Post("/", oc.Create)
		})

		//IMPLEMENTER Routes
		r.Route("/Implementer", func(r chi.Router) {
			r.Post("/", ic.Create)
			r.Route("/{implementerID}/org", func(r chi.Router) {
				r.Use(middleware2.ImplementerCtx)
				r.Get("/", ic.Read)
				r.Post("/", ioc.Create)
			})
		})
		//IMPLEMENTER ORG
		//TODO figure out how to make routes case insensitive and add this route as a sub-route of Implementer
		r.Route("/Implementer/{implementerID}/Org/{organizationID}/system", func(r chi.Router) {
			r.With(middleware2.ImplementerCtx).With(middleware2.OrganizationCtx).Post("/", sc.CreateSystem)
		})
	})
	return r
}

func NewAdminServer() *service.Server {
	attrClient := client.NewAttributionClient(client.AttributionConfig{
		URL:     conf.GetAsString("attribution-client.url"),
		Retries: conf.GetAsInt("attribution-client.retries", 3),
	})

	ssasClient := client.NewSsasHTTPClient(client.SsasHTTPClientConfig{
		URL:          conf.GetAsString("ssas-client.url"),
		Retries:      conf.GetAsInt("ssas-client.attrRetries", 3),
		ClientID:     conf.GetAsString("ssas-client.client-id"),
		ClientSecret: conf.GetAsString("ssas-client.client-secret"),
	})

	port := conf.GetAsInt("ADMIN_PORT", 3011)
	oc := v2.NewOrganizationController(attrClient)
	ic := v2.NewImplementerController(attrClient, ssasClient)
	ioc := v2.NewImplementerOrgController(attrClient)
	sc := v2.NewSSASController(ssasClient, attrClient)
	r := buildAdminRoutes(oc, ic, ioc, sc)
	return service.NewServer("DPC-API Admin Server", port, true, r)
}
