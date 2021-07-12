package v2

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/middleware"
	"github.com/darahayes/go-boom"
	"go.uber.org/zap"
	"net/http"

	"github.com/CMSgov/dpc/api/client"
)

// SSASController is a struct that defines what the controller has
type SSASController struct {
	ssasClient client.SsasClient
	attrClient client.Client
}

// NewSSASController function that creates a ssas controller and returns it's reference
func NewSSASController(ssasClient client.SsasClient, attrClient client.Client) *SSASController {
	return &SSASController{
		ssasClient, attrClient,
	}
}

// function that calls SSAS to create a new system
func (sc *SSASController) CreateSystem(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	implementorID, _ := r.Context().Value(middleware.ContextKeyImplementor).(string)
	organizationID, _ := r.Context().Value(middleware.ContextKeyOrganization).(string)

	if implementorID == "" || organizationID == "" {
		log.Error(fmt.Sprintf("Failed to extract one or more path parameters. ImplID: %s ,OrgID: %s ", implementorID, organizationID))
		boom.Internal(w, w, http.StatusInternalServerError, "Internal Server Error")
		return
	}

	found, mOrg, err := sc.getManagedOrg(r, implementorID, organizationID)
	if err != nil {
		log.Error("Failed to retrieve implementer's managed orgs", zap.Error(err))
		boom.Internal(w, w, http.StatusInternalServerError, "Internal Server Error")
		return
	}

	if !found || "Active" != mOrg.Status {
		log.Error("could not create system for inactive or missing relation")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Implementor/Org relation is not active")
		return
	}

	if mOrg.SsasSystemID != "" {
		log.Error(fmt.Sprintf("realation with implementerID: %s and organizationID: %s already has a system", implementorID, organizationID))
		fhirror.BusinessViolation(r.Context(), w, http.StatusConflict, "a system for this implementer/org relation already exists")
		return
	}

	proxyReq := ProxyCreateSystemRequest{}
	if err := json.NewDecoder(r.Body).Decode(&proxyReq); err != nil {
		log.Error(err.Error())
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to parse request body")
		return
	}

	ssasResp, err := sc.createSsasSystem(r, implementorID, organizationID, proxyReq)
	if err != nil {
		log.Error("Failed to create system", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to create system")
		return
	}
	uRel := client.ImplementerOrg{
		OrgID:         organizationID,
		ImplementerID: implementorID,
		SsasSystemID:  ssasResp.SystemID,
		Status:        "Active",
	}
	_, err = sc.attrClient.UpdateImplementerOrg(r.Context(), implementorID, organizationID, uRel)
	if err != nil {
		log.Error("Failed to update implementer org relation", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to create system")
		return
	}

	proxyResp := ProxyCreateSystemResponse{}
	proxyResp.ClientName = ssasResp.ClientName
	proxyResp.ClientId = ssasResp.ClientID
	proxyResp.ClientToken = ssasResp.ClientToken
	proxyResp.ExpiresAt = ssasResp.ExpiresAt
	proxyResp.IPs = ssasResp.IPs

	respBytes, err := json.Marshal(proxyResp)
	if err != nil {
		log.Error("Failed convert ssas ProxyCreateSystemResponse to bytes", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to create system")
		return
	}

	if _, err := w.Write(respBytes); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to create system")
		return
	}
	//TODO: once all DELETE endpoints are available (in ssas & attribution) we need to implement rollback logic for failure scenarios.
}
func (sc *SSASController) createSsasSystem(r *http.Request, implID string, orgID string, proxyReq ProxyCreateSystemRequest) (client.CreateSystemResponse, error) {
	groupID, err := sc.getGroupID(r, implID)
	if err != nil {
		return client.CreateSystemResponse{}, err
	}

	req := client.CreateSystemRequest{
		ClientName: proxyReq.ClientName,
		GroupID:    groupID,
		PublicKey:  proxyReq.PublicKey,
		IPs:        proxyReq.IPs,
		XData:      fmt.Sprintf("{\"organizationID\": \"%s\"}", orgID),
	}
	return sc.ssasClient.CreateSystem(r.Context(), req)
}

func (sc *SSASController) getManagedOrg(r *http.Request, implID string, orgID string) (bool, client.ManagedOrg, error) {
	orgs, err := sc.attrClient.GetManagedOrgs(r.Context(), implID)
	if err != nil {
		return false, client.ManagedOrg{}, err
	}
	for _, org := range orgs {
		if org.OrgID == orgID {
			return true, org, nil
		}
	}
	return false, client.ManagedOrg{}, errors.New("relation not found")
}

func (sc *SSASController) getGroupID(r *http.Request, implID string) (string, error) {
	respBytes, err := sc.attrClient.Get(r.Context(), client.Implementer, implID)
	if err != nil {
		return "", err
	}
	var v map[string]interface{}
	err = json.NewDecoder(bytes.NewReader(respBytes)).Decode(&v)
	if err != nil {
		return "", err
	}
	return v["ssas_group_id"].(string), nil
}

type ProxyCreateSystemRequest struct {
	ClientName string   `json:"client_name"`
	PublicKey  string   `json:"public_key"`
	IPs        []string `json:"ips"`
}

type ProxyCreateSystemResponse struct {
	ClientId    string   `json:"client_id"`
	ClientName  string   `json:"client_name"`
	IPs         []string `json:"ips"`
	ClientToken string   `json:"client_token"`
	ExpiresAt   string   `json:"expires_at"`
}
