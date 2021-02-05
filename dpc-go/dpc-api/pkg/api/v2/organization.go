package v2

import (
	"context"
	"net/http"

	"github.com/go-chi/chi"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/pkg/api/client"
)

func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), "organization", organizationID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

type OrganizationController struct {
	attributionClient *client.AttributionClient
}

func NewOrganizationController(config *client.AttributionConfig) *OrganizationController {
	attributionClient := client.NewAttributionClient(config)
	return &OrganizationController{
		attributionClient: attributionClient,
	}
}

func (organizationController *OrganizationController) GetOrganization(w http.ResponseWriter, r *http.Request) {
	organizationID := r.Context().Value("organizationID").(string)
	resp, err := organizationController.attributionClient.Get(client.Organization, organizationID)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if _, err = w.Write(resp); err != nil {
		zap.L().Error("Failed to write data", zap.Error(err))
	}
}
