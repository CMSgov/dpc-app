package middleware

import (
	"context"
	"net/http"
)

// RequestIPCtx middleware to extract the requesting IP address from the incoming request and set it into the request context
func RequestIPCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ipAddress := r.Header.Get("X-Forwarded-For")
		if ipAddress == "" {
			ipAddress = r.RemoteAddr
		}
		ctx := context.WithValue(r.Context(), ContextKeyRequestingIP, ipAddress)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
