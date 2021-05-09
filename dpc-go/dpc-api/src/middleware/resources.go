package middleware

import (
	"context"
	"net/http"

	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/go-chi/chi"
)

// OrgHeader is used in place of a auth token until SSAS is implemented
const OrgHeader string = "X-ORG"

type contextKey int

const (
	// ContextKeyOrganization is the key in the context to retrieve the organizationID
	ContextKeyOrganization contextKey = iota
	// ContextKeyFileName is the key in the context to retrieve the fileName
	ContextKeyFileName
)

// OrganizationCtx middleware to extract the organizationID from the chi url param and set it into the request context
func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), ContextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// AuthCtx middleware is placeholder to get org id from token until we do SSAS
func AuthCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		log := logger.WithContext(r.Context())
		organizationID := r.Header.Get(OrgHeader)
		if organizationID == "" {
			log.Error("Missing auth token")
			fhirror.ServerIssue(r.Context(), w, http.StatusForbidden, "Missing auth token")
			return
		}
		ctx := context.WithValue(r.Context(), ContextKeyOrganization, organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func FileNameCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		groupID := chi.URLParam(r, "fileName")
		ctx := context.WithValue(r.Context(), ContextKeyFileName, groupID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
