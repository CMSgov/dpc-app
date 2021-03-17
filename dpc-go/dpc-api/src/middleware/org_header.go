package middleware

import (
	"context"
	"net/http"
)

type contextKey int

// ContextKeyOrganization is the key in the context to retrieve the organizationID
const ContextKeyOrganization contextKey = iota

// OrgHeader middleware to extract the organizationID from the `X-Org` header and set it into the request context
// This is intended to be a temporary solution until the Auth middleware is implemented.
// TODO: Replace with Auth middleware
func OrgHeader(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := r.Header.Get("X-Org")
		if organizationID != "" {
			ctx := context.WithValue(r.Context(), ContextKeyOrganization, organizationID)
			next.ServeHTTP(w, r.WithContext(ctx))
		}
	})
}
