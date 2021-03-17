package auth

import (
	"net/http"

	"github.com/go-chi/chi"
)

// NewAuthRouter builds the router for auth actions
func NewAuthRouter(middlewares ...func(http.Handler) http.Handler) http.Handler {
	r := chi.NewRouter()
	r.Use(middlewares...)
	r.Post("/auth/token", GetAuthToken)
	// r.With(ParseToken, RequireTokenAuth, CheckBlacklist).Get("/auth/welcome", Welcome)
	r.Get("/auth/welcome", Welcome)
	return r
}
