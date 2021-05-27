package middleware

import (
	"context"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/darahayes/go-boom"
	"github.com/go-chi/chi"
	"github.com/pkg/errors"
	"net/http"
)

// FileNameCtx middleware to extract the fileName from the chi url param and set it into the request context
func FileNameCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fileName := chi.URLParam(r, "fileName")
		ctx := context.WithValue(r.Context(), ContextKeyFileName, fileName)
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

// ImplementerCtx middleware to extract the ImplementerID from the chi url param and set it into the request context
func ImplementerCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ImplementerID := chi.URLParam(r, "implementerID")
		ctx := context.WithValue(r.Context(), ContextKeyImplementer, ImplementerID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

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

// RequestIPCtx middleware to extract the requesting IP address from the incoming request and set it into the request context
func RequestIPCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ipAddress := r.Header.Get(FwdHeader)
		if ipAddress == "" {
			ipAddress = r.RemoteAddr
		}
		ctx := context.WithValue(r.Context(), ContextKeyRequestingIP, ipAddress)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// RequestURLCtx middleware to extract the requesting URL from the incoming request and set it in the request context
func RequestURLCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		requestURL := r.Header.Get(RequestURLHeader)
		if requestURL == "" {
			requestURL = r.RequestURI
		}
		ctx := context.WithValue(r.Context(), ContextKeyRequestURL, requestURL)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
