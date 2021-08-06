package v2

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"

	"io/ioutil"

	"github.com/CMSgov/dpc/api/fhirror"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/model"
	"github.com/darahayes/go-boom"
	"go.uber.org/zap"

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

// CreateToken function that calls SSAS to get a system
func (sc *SSASController) CreateToken(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	implementerID, _ := r.Context().Value(middleware.ContextKeyImplementer).(string)
	organizationID, _ := r.Context().Value(middleware.ContextKeyOrganization).(string)

	if implementerID == "" || organizationID == "" {
		log.Error(fmt.Sprintf("Failed to extract one or more path parameters. ImplID: %s ,OrgID: %s ", implementerID, organizationID))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	found, mOrg, err := sc.getProviderOrg(r, implementerID, organizationID)
	if err != nil {
		log.Error("Failed to retrieve implementer's managed orgs", zap.Error(err))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	if !found || "Active" != mOrg.Status {
		log.Error("Could not find active org")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Implementer/Org relation is not active")
		return
	}

	if mOrg.SsasSystemID == "" {
		log.Error(fmt.Sprintf("relation with implementerID: %s and organizationID: %s is not tied to a system", implementerID, organizationID))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "a system was not found for this implementer/org relationship")
		return
	}

	b, err := ioutil.ReadAll(r.Body)
	if err != nil {
		log.Error("Failed to read body", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to create token")
		return
	}

	var resp TokenCreateRequest
	if err := json.Unmarshal(b, &resp); err != nil {
		log.Error("Failed to unmarshal response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to create token")
		return
	}

	if resp.Label == "" {
		resp.Label = fmt.Sprintf("Initial %s token", mOrg.OrgName)
	}

	token, err := sc.ssasClient.CreateToken(r.Context(), mOrg.SsasSystemID, resp.Label)
	if err != nil {
		log.Error("Failed to create token", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to create token")
		return
	}

	if _, err := w.Write([]byte(token)); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}
}

// DeleteToken function that calls SSAS to get a system
func (sc *SSASController) DeleteToken(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	implementerID, _ := r.Context().Value(middleware.ContextKeyImplementer).(string)
	organizationID, _ := r.Context().Value(middleware.ContextKeyOrganization).(string)
	tokenID, _ := r.Context().Value(middleware.ContextKeyTokenID).(string)

	if implementerID == "" || organizationID == "" {
		log.Error(fmt.Sprintf("Failed to extract one or more path parameters. ImplID: %s ,OrgID: %s ", implementerID, organizationID))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	found, mOrg, err := sc.getProviderOrg(r, implementerID, organizationID)
	if err != nil {
		log.Error("Failed to retrieve implementer's managed orgs", zap.Error(err))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	if !found || "Active" != mOrg.Status {
		log.Error("Could not find active org")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Implementer/Org relation is not active")
		return
	}

	if mOrg.SsasSystemID == "" {
		log.Error(fmt.Sprintf("relation with implementerID: %s and organizationID: %s is not tied to a system", implementerID, organizationID))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "a system was not found for this implementer/org relationship")
		return
	}

	err = sc.ssasClient.DeleteToken(r.Context(), mOrg.SsasSystemID, tokenID)
	if err != nil {
		log.Error("Failed to delete token", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to delete token")
		return
	}

	w.WriteHeader(http.StatusOK)
}

// GetSystem function that calls SSAS to get a system
func (sc *SSASController) GetSystem(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	implementerID, _ := r.Context().Value(middleware.ContextKeyImplementer).(string)
	organizationID, _ := r.Context().Value(middleware.ContextKeyOrganization).(string)

	if implementerID == "" || organizationID == "" {
		log.Error(fmt.Sprintf("Failed to extract one or more path parameters. ImplID: %s ,OrgID: %s ", implementerID, organizationID))
		boom.Internal(w, w, http.StatusInternalServerError, "Internal Server Error")
		return
	}

	found, mOrg, err := sc.getProviderOrg(r, implementerID, organizationID)
	if err != nil {
		log.Error("Failed to retrieve implementer's managed orgs", zap.Error(err))
		boom.Internal(w, w, http.StatusInternalServerError, "Internal Server Error")
		return
	}

	if !found || "Active" != mOrg.Status {
		log.Error("could not create system for inactive or missing relation")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Implementer/Org relation is not active")
		return
	}

	if mOrg.SsasSystemID == "" {
		log.Error(fmt.Sprintf("realation with implementerID: %s and organizationID: %s is not tied to a system", implementerID, organizationID))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "a system was not found for this implementer/org relationship")
		return
	}

	ssasResp, err := sc.ssasClient.GetSystem(r.Context(), mOrg.SsasSystemID)
	if err != nil {
		log.Error("Failed to get system", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to get system")
		return
	}

	proxyResp := ProxyGetSystemResponse{}
	proxyResp.ClientName = ssasResp.ClientName
	proxyResp.ClientID = ssasResp.ClientID
	proxyResp.ClientTokens = ssasResp.ClientTokens
	proxyResp.PublicKeys = ssasResp.PublicKeys
	proxyResp.IPs = ssasResp.IPs

	respBytes, err := json.Marshal(proxyResp)
	if err != nil {
		log.Error("Failed convert ssas ProxyGetSystemResponse to bytes", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to get system")
		return
	}

	if _, err := w.Write(respBytes); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to get system")
		return
	}

}

// DeleteKey function to delete a public key from ssas system
func (sc *SSASController) DeleteKey(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	implementerID, _ := r.Context().Value(middleware.ContextKeyImplementer).(string)
	organizationID, _ := r.Context().Value(middleware.ContextKeyOrganization).(string)
	keyID, _ := r.Context().Value(middleware.ContextKeyKeyID).(string)

	if implementerID == "" || organizationID == "" || keyID == "" {
		log.Error(fmt.Sprintf("Failed to extract one or more path parameters. ImplID: %s, OrgID: %s, KeyID: %s ", implementerID, organizationID, keyID))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	found, mOrg, err := sc.getProviderOrg(r, implementerID, organizationID)
	if err != nil {
		log.Error("Failed to retrieve implementer's managed orgs", zap.Error(err))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	if !found || "Active" != mOrg.Status {
		log.Error("Could not find active org")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "ImplementerID/Org relation is not active")
		return
	}

	if mOrg.SsasSystemID == "" {
		log.Error(fmt.Sprintf("relation with implementerID: %s and organizationID: %s is not tied to a system", implementerID, organizationID))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "a system was not found for this implementer/org relationship")
		return
	}

	err = sc.ssasClient.DeletePublicKey(r.Context(), mOrg.SsasSystemID, keyID)
	if err != nil {
		log.Error("Failed to delete key", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to delete key")
		return
	}

	w.WriteHeader(http.StatusOK)
}

// AddKey function to add a public key to ssas system
func (sc *SSASController) AddKey(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	implementerID, _ := r.Context().Value(middleware.ContextKeyImplementer).(string)
	organizationID, _ := r.Context().Value(middleware.ContextKeyOrganization).(string)

	if implementerID == "" || organizationID == "" {
		log.Error(fmt.Sprintf("Failed to extract one or more path parameters. ImplID: %s ,OrgID: %s ", implementerID, organizationID))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	found, mOrg, err := sc.getProviderOrg(r, implementerID, organizationID)
	if err != nil {
		log.Error("Failed to retrieve implementer's managed orgs", zap.Error(err))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	if !found || "Active" != mOrg.Status {
		log.Error("Could not find active org")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Implementer/Org relation is not active")
		return
	}

	if mOrg.SsasSystemID == "" {
		log.Error(fmt.Sprintf("relation with implementerID: %s and organizationID: %s is not tied to a system", implementerID, organizationID))
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "a system was not found for this implementer/org relationship")
		return
	}

	proxyReq := model.ProxyPublicKeyRequest{}
	if err := json.NewDecoder(r.Body).Decode(&proxyReq); err != nil {
		log.Error(err.Error())
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to parse request body")
		return
	}

	if proxyReq.Signature == "" {
		log.Error("Signature is required when adding a public key")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Signature is required when adding a public key")
		return
	}

	ssasResp, err := sc.ssasClient.AddPublicKey(r.Context(), mOrg.SsasSystemID, proxyReq)
	if err != nil {
		log.Error("Failed to add key", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to add key")
		return
	}

	b, err := json.Marshal(ssasResp)
	if err != nil {
		log.Error("Failed to unmarshal", zap.Error(err))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}

	if _, err := w.Write(b); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.GenericServerIssue(r.Context(), w)
		return
	}
}

// CreateSystem function that calls SSAS to create a new system
func (sc *SSASController) CreateSystem(w http.ResponseWriter, r *http.Request) {
	log := logger.WithContext(r.Context())
	implementerID, _ := r.Context().Value(middleware.ContextKeyImplementer).(string)
	organizationID, _ := r.Context().Value(middleware.ContextKeyOrganization).(string)

	if implementerID == "" || organizationID == "" {
		log.Error(fmt.Sprintf("Failed to extract one or more path parameters. ImplID: %s ,OrgID: %s ", implementerID, organizationID))
		boom.Internal(w, w, http.StatusInternalServerError, "Internal Server Error")
		return
	}

	found, mOrg, err := sc.getProviderOrg(r, implementerID, organizationID)
	if err != nil {
		log.Error("Failed to retrieve implementer's managed orgs", zap.Error(err))
		boom.Internal(w, w, http.StatusInternalServerError, "Internal Server Error")
		return
	}

	if !found || "Active" != mOrg.Status {
		log.Error("could not create system for inactive or missing relation")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Implementer/Org relation is not active")
		return
	}

	if mOrg.SsasSystemID != "" {
		log.Error(fmt.Sprintf("realation with implementerID: %s and organizationID: %s already has a system", implementerID, organizationID))
		fhirror.BusinessViolation(r.Context(), w, http.StatusConflict, "a system for this implementer/org relation already exists")
		return
	}

	proxyReq := ProxyCreateSystemRequest{}
	if err := json.NewDecoder(r.Body).Decode(&proxyReq); err != nil {
		log.Error(err.Error())
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Failed to parse request body")
		return
	}

	if proxyReq.Signature == "" {
		log.Error("Signature is required when creating a system")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Signature is required when creating a system")
		return
	}

	ssasResp, err := sc.createSsasSystem(r, implementerID, organizationID, proxyReq)
	if err != nil {
		log.Error("Failed to create system", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to create system")
		return
	}
	uRel := client.ImplementerOrg{
		OrgID:         organizationID,
		ImplementerID: implementerID,
		SsasSystemID:  ssasResp.SystemID,
		Status:        "Active",
	}
	_, err = sc.attrClient.UpdateImplementerOrg(r.Context(), implementerID, organizationID, uRel)
	if err != nil {
		log.Error("Failed to update implementer org relation", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, 500, "Failed to create system")
		return
	}

	proxyResp := ProxyCreateSystemResponse{}
	proxyResp.ClientName = ssasResp.ClientName
	proxyResp.ClientID = ssasResp.ClientID
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

// GetAuthToken proxies a request to get an auth token from the SSAS service
func (sc *SSASController) GetAuthToken(w http.ResponseWriter, r *http.Request) {
	body, _ := ioutil.ReadAll(r.Body)
	log := logger.WithContext(r.Context())

	if len(body) == 0 {
		log.Error("Body is empty")
		fhirror.BusinessViolation(r.Context(), w, http.StatusBadRequest, "Body is required")
		return
	}

	// TODO: May need to bring up error codes for more specific errors for troubleshooting
	resBytes, err := sc.ssasClient.Authenticate(r.Context(), body)
	if err != nil {
		log.Error("Failed to authenticate", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, fmt.Sprintf("Failed to authenticate token: %s", err))
		return
	}

	if len(resBytes) <= 0 {
		log.Error("No token returned from SSAS")
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "No token returned from SSAS")
		return
	}

	if _, err := w.Write(resBytes); err != nil {
		log.Error("Failed to write data to response", zap.Error(err))
		fhirror.ServerIssue(r.Context(), w, http.StatusInternalServerError, "Failed to authenticate token")
	}
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

func (sc *SSASController) getProviderOrg(r *http.Request, implID string, orgID string) (bool, client.ProviderOrg, error) {
	orgs, err := sc.attrClient.GetProviderOrgs(r.Context(), implID)
	if err != nil {
		return false, client.ProviderOrg{}, err
	}
	for _, org := range orgs {
		if org.OrgID == orgID {
			return true, org, nil
		}
	}
	return false, client.ProviderOrg{}, nil
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

// ProxyCreateSystemRequest struct that models a proxy request to create a new system
type ProxyCreateSystemRequest struct {
	ClientName string   `json:"client_name"`
	PublicKey  string   `json:"public_key"`
	Signature  string   `json:"signature"`
	IPs        []string `json:"ips"`
}

// ProxyCreateSystemResponse struct that models a proxy response to create a new system
type ProxyCreateSystemResponse struct {
	ClientID    string   `json:"client_id"`
	ClientName  string   `json:"client_name"`
	IPs         []string `json:"ips,omitempty"`
	ClientToken string   `json:"client_token"`
	ExpiresAt   string   `json:"expires_at"`
}

// ProxyGetSystemResponse struct that models a proxy response to get a system
type ProxyGetSystemResponse struct {
	ClientID     string              `json:"client_id"`
	ClientName   string              `json:"client_name"`
	PublicKeys   []map[string]string `json:"public_keys"`
	IPs          []map[string]string `json:"ips"`
	ClientTokens []map[string]string `json:"client_tokens"`
}

// TokenCreateRequest struct that models the creat request for a token
type TokenCreateRequest struct {
	Label string `json:"label"`
}
