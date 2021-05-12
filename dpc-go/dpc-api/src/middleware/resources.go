package middleware

import (
	"context"
	"net/http"

	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/go-chi/chi"
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

// GroupCtx middleware to extract the groupID from the chi url param and set it into the request context
func GroupCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		groupID := chi.URLParam(r, "groupID")
		ctx := context.WithValue(r.Context(), ContextKeyGroup, groupID)
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
