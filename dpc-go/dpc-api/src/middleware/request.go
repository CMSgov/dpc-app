package middleware

import (
	"context"
	"fmt"
	"net/http"
)

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
		scheme := "http"
		if r.TLS != nil {
			scheme = "https"
		}
		ctx := context.WithValue(r.Context(), ContextKeyRequestURL, fmt.Sprintf("%s://%s%s %s\" ", scheme, r.Host, r.RequestURI, r.Proto))
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
