package middleware

import (
	"context"
	"net/http"

	"github.com/go-chi/chi"
)

// ContextKeyGroup is the key in the context to retrieve the groupID
const ContextKeyGroup ContextKey = iota

// GroupCtx middleware to extract the groupID from the chi url param and set it into the request context
func GroupCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		groupID := chi.URLParam(r, "groupID")
		ctx := context.WithValue(r.Context(), ContextKeyGroup, groupID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
