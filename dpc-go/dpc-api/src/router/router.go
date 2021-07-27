package router

import (
	"net/http"
	"strings"

	"github.com/go-chi/chi/middleware"

	"github.com/CMSgov/dpc/api/auth"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	v2 "github.com/CMSgov/dpc/api/v2"

	"github.com/go-chi/chi"
)

// Controllers collects the various controllers needed for the DPC API router
type Controllers struct {
	Org      v2.Controller
	Metadata v2.ReadController
	Group    v2.Controller
	Data     v2.FileController
	Job      v2.JobController
	Impl     v2.Controller
	ImplOrg  v2.Controller
	Ssas     v2.AuthController
}

// NewDPCAPIRouter function that builds the router using chi
func NewDPCAPIRouter(rc Controllers) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware2.RequestIPCtx)
	fileServer(r, "/v2/swagger", http.Dir("../swaggerui"))
	r.
		With(middleware2.Sanitize).
		Route("/v2", func(r chi.Router) {
			r.Use(middleware.SetHeader("Content-Type", "application/fhir+json; charset=UTF-8"))
			r.Get("/metadata", rc.Metadata.Read)
			r.Route("/Organization", func(r chi.Router) {
				r.Route("/{organizationID}", func(r chi.Router) {
					r.Use(middleware2.OrganizationCtx)
					r.With(middleware2.FHIRModel).Get("/", rc.Org.Read)
					r.Delete("/", rc.Org.Delete)
					r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Put("/", rc.Org.Update)
				})
				r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Post("/", rc.Org.Create)
			})
			r.Route("/Group", func(r chi.Router) {
				r.Use(middleware2.AuthCtx)
				r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Post("/", rc.Group.Create)
				r.Route("/{groupID}", func(r chi.Router) {
					r.Use(middleware2.RequestURLCtx)
					r.Use(middleware2.GroupCtx)
					r.Use(middleware2.ExportTypesParamCtx)
					r.Use(middleware2.ExportSinceParamCtx)
					r.Get("/$export", rc.Group.Export)
				})
			})
			r.Route("/Implementer", func(r chi.Router) {
				r.Use(middleware2.AuthCtx)
				r.Post("/", rc.Impl.Create)
				r.Route("/{implementerID}/org", func(r chi.Router) {
					r.Use(middleware2.ImplementerCtx)
					r.Get("/", rc.ImplOrg.Read)
					r.Post("/", rc.ImplOrg.Create)
				})
			})
			r.Route("/Jobs", func(r chi.Router) {
				r.Use(middleware.SetHeader("Content-Type", "application/json; charset=UTF-8"))
				r.Use(middleware2.AuthCtx)
				r.With(middleware2.JobCtx).Get("/{jobID}", rc.Job.Status)
			})
			r.Route("/Data", func(r chi.Router) {
				r.Use(middleware2.AuthCtx)
				r.With(middleware2.FileNameCtx).Get("/{fileName}", rc.Data.GetFile)
			})
			r.Route("/Implementer/{implementerID}/Org/{organizationID}/system", func(r chi.Router) {
				r.With(middleware2.ImplementerCtx).With(middleware2.OrganizationCtx).Post("/", rc.Ssas.CreateSystem)
				r.With(middleware2.ImplementerCtx, middleware2.OrganizationCtx).Get("/", rc.Ssas.GetSystem)
			})
			r.Post("/Token/auth", rc.Ssas.GetAuthToken)
		})
	r.Get("/auth/welcome", auth.Welcome)
	return r
}

func fileServer(r chi.Router, path string, root http.FileSystem) {
	if strings.ContainsAny(path, "{}*") {
		panic("FileServer does not permit URL parameters.")
	}

	fs := http.StripPrefix(path, http.FileServer(root))

	if path != "/" && path[len(path)-1] != '/' {
		r.Get(path, http.RedirectHandler(path+"/", 301).ServeHTTP)
		path += "/"
	}
	path += "*"

	r.Get(path, func(w http.ResponseWriter, r *http.Request) {
		fs.ServeHTTP(w, r)
	})
}
