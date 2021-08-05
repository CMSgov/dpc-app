package router

import (
	"net/http"

	v1 "github.com/CMSgov/dpc/attribution/service/v1"

	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/service"
	"github.com/go-chi/chi"
	"github.com/go-chi/chi/middleware"
	"github.com/go-chi/render"
)

// NewDPCAttributionRouter function to build the attribution router
func NewDPCAttributionRouter(o service.Service, g service.Service, impl service.Service, implOrg service.Service, d v1.DataService, js v1.JobService) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware.SetHeader("Content-Type", "application/json; charset=UTF-8"))
	r.Route("/", func(r chi.Router) {
		r.Get("/_health", getHealthCheck)
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(middleware2.OrganizationCtx)
				r.Get("/", o.Get)
				r.Delete("/", o.Delete)
				r.Put("/", o.Put)
			})
			r.Post("/", o.Post)
		})
		r.Route("/Group", func(r chi.Router) {
			r.Use(middleware2.AuthCtx)
			r.Post("/", g.Post)
			r.Route("/{groupID}", func(r chi.Router) {
				r.Use(middleware2.GroupCtx)
				r.Get("/", g.Get)
			})
		})
		r.Route("/Implementer", func(r chi.Router) {
			r.Post("/", impl.Post)
			r.Route("/{implementerID}", func(r chi.Router) {
				r.Use(middleware2.ImplementerCtx)
				r.Put("/", impl.Put)
				r.Get("/", impl.Get)
			})
			r.Route("/{implementerID}/org", func(r chi.Router) {
				r.Use(middleware2.ImplementerCtx)
				r.Post("/", implOrg.Post)
				r.Get("/", implOrg.Get)
				r.Delete("/", implOrg.Delete)
				r.Put("/", implOrg.Put)
				r.Route("/{organizationID}", func(r chi.Router) {
					r.Use(middleware2.OrganizationCtx)
					r.Put("/", implOrg.Put)
				})
			})
		})

		//Go away once shared job service
		r.Route("/Data", func(r chi.Router) {
			r.Use(middleware2.AuthCtx)
			r.With(middleware2.FileNameCtx).Get("/validityCheck/{fileName}", d.GetFileInfo)
		})

		//Go away once shared job service
		r.Route("/Job", func(r chi.Router) {
			r.Use(middleware2.AuthCtx)
			r.With(middleware2.JobCtx).Get("/{jobID}", js.BatchesAndFiles)
			r.Post("/", js.Export)
		})
	})

	return r
}

func getHealthCheck(w http.ResponseWriter, r *http.Request) {
	m := make(map[string]string)
	m["api"] = "ok"
	w.WriteHeader(http.StatusOK)
	render.JSON(w, r, m)
}
