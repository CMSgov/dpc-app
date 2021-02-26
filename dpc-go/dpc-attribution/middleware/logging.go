package middleware

import (
	"fmt"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/go-chi/chi"
	"github.com/go-chi/chi/middleware"
	"go.uber.org/zap"
	"net/http"
)

// Logging function to return a chain of predefined middleware (chi.RequestID & custom request/response logging)
// This middleware wil ensure the logs contain the rqId (correlation) and logs the beginning and end of the request
func Logging() func(next http.Handler) http.Handler {
	lh := func(next http.Handler) http.Handler {
		fn := func(w http.ResponseWriter, r *http.Request) {
			ww := middleware.NewWrapResponseWriter(w, r.ProtoMajor)
			ctx := logger.NewContext(r.Context(), zap.String("rqId", r.Context().Value(middleware.RequestIDKey).(string)))
			req := r.WithContext(ctx)
			requestLog(req)
			defer func() {
				responseLog(ww, req)
			}()
			next.ServeHTTP(ww, req)
		}
		return http.HandlerFunc(fn)
	}
	return chi.Chain(middleware.RequestID, lh).Handler
}

func requestLog(r *http.Request) {
	log := logger.WithContext(r.Context())
	scheme := "http"
	if r.TLS != nil {
		scheme = "https"
	}
	log.Info("Starting request",
		zap.String("request-uri", fmt.Sprintf("%s://%s%s %s\" ", scheme, r.Host, r.RequestURI, r.Proto)),
		zap.String("from", r.RemoteAddr),
		zap.String("method", r.Method))
}

func responseLog(ww middleware.WrapResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	log.Info("Finishing request",
		zap.Int("response-code", ww.Status()),
		zap.Any("bytes", ww.BytesWritten()))

}
