package v2

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/logger"
	"github.com/CMSgov/dpc/attribution/orm"
	"github.com/CMSgov/dpc/attribution/util"
	"github.com/darahayes/go-boom"
	"github.com/go-chi/chi"
	"github.com/go-pg/pg/v10"
	"github.com/pkg/errors"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
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
	db *pg.DB
}

func NewOrganizationController() *OrganizationController {
	return &OrganizationController{
		db: orm.GetDbConnection(),
	}
}

func (oc *OrganizationController) GetOrganization(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	organizationID, ok := r.Context().Value(contextKeyOrganization).(string)
	if !ok {
		log.Error("Failed to extract organization id from context")
		boom.BadData(w, "Could not get organization id")
		return
	}

	//Call database and get stuff
	org := &orm.Organization{ID: organizationID}
	err := oc.db.Model(org).WherePK().Select()
	if err != nil {
		log.Error(fmt.Sprintf("Failed to retrieve organization"), zap.Error(err))
		boom.NotFound(w, err.Error())
		return
	}

	orgBytes := new(bytes.Buffer)
	if err := json.NewEncoder(orgBytes).Encode(org); err != nil {
		log.Error(fmt.Sprintf("Failed to convert orm model to bytes for organization"), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if _, err := w.Write(orgBytes.Bytes()); err != nil {
		log.Error(fmt.Sprintf("Failed to write organization to response for organization"), zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

func (oc *OrganizationController) SaveOrganization(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	body, _ := ioutil.ReadAll(r.Body)

	err := oc.npiExists(body)
	if err != nil {
		log.Error("NPI already exists", zap.Error(err))
		boom.BadRequest(w, err.Error())
		return
	}

	var result map[string]interface{}
	if err := json.Unmarshal(body, &result); err != nil {
		log.Error("Failed to unmarshal organization", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	org := &orm.Organization{
		Info: result,
	}

	_, err = oc.db.Model(org).Insert()
	if err != nil {
		log.Error("Failed to create organization", zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	orgBytes := new(bytes.Buffer)
	if err := json.NewEncoder(orgBytes).Encode(org); err != nil {
		log.Error(fmt.Sprintf("Failed to convert orm model to bytes for organization"), zap.Error(err))
		boom.Internal(w, err.Error())
		return
	}

	w.Header().Set("Content-Type", "application/json")
	if _, err := w.Write(orgBytes.Bytes()); err != nil {
		log.Error(fmt.Sprintf("Failed to write organization to response for organization"), zap.Error(err))
		boom.Internal(w, err.Error())
	}
}

func (oc *OrganizationController) npiExists(b []byte) error {
	npi, err := util.GetNPI(b)
	if err != nil {
		return err
	}
	count, err := oc.db.Model((*orm.Organization)(nil)).Where("info @> '{\"identifier\": [{\"value\": ?}]}'", pg.Ident(npi)).Count()
	if err != nil {
		return err
	}
	if count != 0 {
		return errors.New("NPI already exists")
	}
	return nil
}
