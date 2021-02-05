package v2

import (
	"context"
    "fmt"
    "net/http"

	"github.com/go-chi/chi"
	"go.uber.org/zap"
)

func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), "organization", organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

type OrganizationController struct {
}

func NewOrganizationController() *OrganizationController {
	return &OrganizationController{
	}
}

func (organizationController *OrganizationController) GetOrganization(w http.ResponseWriter, r *http.Request) {
	organizationID := r.Context().Value("organizationID").(string)
    zap.L().Info(fmt.Sprintf("organization id being requested %s", organizationID))

	//Call database and get stuff

	w.Header().Set("Content-Type", "application/json")
	if _, err := w.Write(nil); err != nil {
		zap.L().Error("Failed to write data", zap.Error(err))
	}
}
