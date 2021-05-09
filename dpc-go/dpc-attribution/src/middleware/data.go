package middleware

import (
	"context"
	"github.com/go-chi/chi"
	"net/http"
)

// GroupCtx middleware to extract the groupID from the chi url param and set it into the request context
func FileNameCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fileName := chi.URLParam(r, "fileName")
		ctx := context.WithValue(r.Context(), ContextKeyFileName, fileName)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
