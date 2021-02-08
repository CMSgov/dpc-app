package v2

import (
	"context"
	"github.com/pkg/errors"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"io/ioutil"
	"net/http"

	"github.com/go-chi/chi"
	"go.uber.org/zap"

	"github.com/CMSgov/dpc/api/pkg/client"
)

type contextKey string

func (c contextKey) String() string {
	return string(c)
}

var (
	contextKeyOrganization = contextKey("organization")
)

func OrganizationCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		organizationID := chi.URLParam(r, "organizationID")
		ctx := context.WithValue(r.Context(), contextKeyOrganization, organizationID)
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
	organizationID, ok := r.Context().Value(contextKeyOrganization).(string)
	if !ok {
		http.Error(w, "Failed to get organization id", http.StatusInternalServerError)
		return
	}

	resp, err := organizationController.attributionClient.Get(client.Organization, organizationID)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if err = isValidOrganization(resp); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	o, err := unmarshalMarshal(resp)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if _, err = w.Write(o); err != nil {
		zap.L().Error("Failed to write data to response", zap.Error(err))
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}

func (organizationController *OrganizationController) CreateOrganization(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	if err := isValidOrganization(body); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	resp, err := organizationController.attributionClient.Post(client.Organization, body)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	o, err := unmarshalMarshal(resp)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if _, err = w.Write(o); err != nil {
		zap.L().Error("Failed to write data to response", zap.Error(err))
		http.Error(w, err.Error(), http.StatusInternalServerError)
	}
}

//Unmarshal and marshall using fhir library to get the json keys in a good order
//doesn't matter as it only makes the json prettier
func unmarshalMarshal(resp []byte) ([]byte, error) {
	o, err := fhir.UnmarshalOrganization(resp)
	if err != nil {
		return nil, err
	}
	b, err := o.MarshalJSON()
	if err != nil {
		return nil, err
	}
	return b, nil
}
func isValidOrganization(org []byte) error {
	_, err := fhir.UnmarshalOrganization(org)
	if err != nil {
		zap.L().Error("Organization is not a properly formed FHIR object", zap.Error(err))
		return errors.New("Organization is not a properly formed FHIR object")
	}
	return nil
}
