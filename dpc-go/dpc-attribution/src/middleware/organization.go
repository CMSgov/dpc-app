package middleware

import (
	"context"
	"net/http"

	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/darahayes/go-boom"
	"github.com/go-chi/chi"
	"github.com/pkg/errors"
)

// OrgHeader is the header to look for when api calls attribution with a organization id
const OrgHeader string = "X-ORG"

// OrganizationCtx middleware to extract the organizationID from the header and set it into the request context
func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), ContextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// AuthCtx middleware is placeholder to get org id from token
func AuthCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		log := logger.WithContext(r.Context())
		organizationID := r.Header.Get(OrgHeader)
		if organizationID == "" {
			log.Error("Missing auth token")
			boom.Forbidden(w, errors.New("Missing auth token"))
			return
		}
		ctx := context.WithValue(r.Context(), ContextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
