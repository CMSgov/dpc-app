package client

import (
	"bytes"
	"context"
	"crypto/tls"
	"crypto/x509"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/logger"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/go-chi/chi/middleware"
	"io/ioutil"
	"net/http"

	"github.com/hashicorp/go-retryablehttp"
	"github.com/pkg/errors"
	"go.uber.org/zap"
)

// AttributionConfig is a struct to hold configuration info for retryablehttp client
type AttributionConfig struct {
	URL        string
	Retries    int
	CACertPool *x509.CertPool
	Cert       tls.Certificate
}

// ResourceType is a type to be used when making requests to different endpoints in attribution service
type ResourceType string

// Contains the different ResourceType for calls to attribution
const (
	Organization ResourceType = "Organization"
	Group        ResourceType = "Group"
	Implementer  ResourceType = "Implementer"
)

// ImplementerOrg struct representing an ImplementerOrg relation
type ImplementerOrg struct {
	ID            string `json:"id" faker:"uuid_hyphenated"`
	OrgID         string `json:"org_id" faker:"uuid_hyphenated"`
	ImplementerID string `json:"implementer_id" faker:"uuid_hyphenated"`
	SsasSystemID  string `json:"ssas_system_id" faker:"-"`
	Status        string `json:"status" faker:"oneof:Active"`
	Npi           string `json:"npi" faker:"-"`
}

// ProviderOrg struct representing an org managed by an implementer
type ProviderOrg struct {
	OrgName      string `json:"org_name" faker:"word"`
	OrgID        string `json:"org_id" faker:"uuid_hyphenated"`
	Npi          string `json:"npi" faker:"-"`
	Status       string `json:"status" faker:"oneof:Active"`
	SsasSystemID string `json:"ssas_system_id" faker:"-"`
}

// Client interface for testing purposes
type Client interface {
	Get(ctx context.Context, resourceType ResourceType, id string) ([]byte, error)
	Post(ctx context.Context, resourceType ResourceType, body []byte) ([]byte, error)
	Delete(ctx context.Context, resourceType ResourceType, id string) error
	Put(ctx context.Context, resourceType ResourceType, id string, body []byte) ([]byte, error)
	Export(ctx context.Context, resourceType ResourceType, id string) ([]byte, error)
	UpdateImplOrg(ctx context.Context, implID string, orgID string, rel ImplementerOrg) (ImplementerOrg, error)
	GetProviderOrgs(ctx context.Context, implID string) ([]ProviderOrg, error)
	CreateImplOrg(ctx context.Context, body []byte) (ImplementerOrg, error)
	GetImplOrg(ctx context.Context) ([]byte, error)
}

// AttributionClient is a struct to hold the retryablehttp client and configs
type AttributionClient struct {
	config     AttributionConfig
	httpClient *retryablehttp.Client
}

// NewAttributionClient initializes the retryable client and returns a reference to the attribution client
func NewAttributionClient(config AttributionConfig) Client {
	client := retryablehttp.NewClient()
	client.RetryMax = config.Retries

	if len(config.Cert.Certificate) != 0 && config.CACertPool != nil {
		client.HTTPClient.Transport = &http.Transport{
			TLSClientConfig: &tls.Config{
				RootCAs:      config.CACertPool,
				Certificates: []tls.Certificate{config.Cert},
			},
		}
	}

	return &AttributionClient{
		config:     config,
		httpClient: client,
	}
}

// CreateImplOrg is a function to create an Implementer/Organization relation via attribution service
func (ac *AttributionClient) CreateImplOrg(ctx context.Context, body []byte) (ImplementerOrg, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	implID, ok := ctx.Value(middleware2.ContextKeyImplementer).(string)
	if !ok {
		log.Error("Failed to extract the implementer id from the context")
		return ImplementerOrg{}, errors.Errorf("Failed to extract the implementer id from the context")
	}

	url := fmt.Sprintf("%s/Implementer/%s/org", ac.config.URL, implID)
	req, err := retryablehttp.NewRequest(http.MethodPost, url, body)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return ImplementerOrg{}, errors.Errorf("Failed to create request")
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	if ctx.Value(middleware2.ContextKeyOrganization) != nil {
		req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	}
	resp, err := ac.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return ImplementerOrg{}, errors.Errorf("Failed to send request")
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		log.Error(fmt.Sprintf("Failed to send request. Status code %d", resp.StatusCode))
		return ImplementerOrg{}, errors.Errorf("Failed to save resource")
	}

	defer func() {
		err := resp.Body.Close()
		if err != nil {
			log.Error("Failed to close response body", zap.Error(err))
		}
	}()

	implOrg := ImplementerOrg{}
	if err := json.NewDecoder(resp.Body).Decode(&implOrg); err != nil {
		log.Error("Failed to convert bytes to ImplementerOrg model", zap.Error(err))
		return ImplementerOrg{}, errors.Errorf("Failed to update implementerOrg relation")
	}

	return implOrg, nil
}

// GetImplOrg calls attribution service via GET to return the Organizations associated with an Implementer
func (ac *AttributionClient) GetImplOrg(ctx context.Context) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	implID, ok := ctx.Value(middleware2.ContextKeyImplementer).(string)
	if !ok {
		log.Error("Failed to extract the implementer id from the context")
		return nil, errors.Errorf("Failed to extract the implementer id from the context")
	}

	url := fmt.Sprintf("%s/implementer/%s/org", ac.config.URL, implID)
	req, err := retryablehttp.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to create request")
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	if ctx.Value(middleware2.ContextKeyOrganization) != nil {
		req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	}
	resp, err := ac.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to send request")
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		log.Error(fmt.Sprintf("Failed to get organizations for implementer %s. Status code %d", implID, resp.StatusCode))
		return nil, errors.Errorf("Failed to retrieve resource")
	}

	defer func() {
		err := resp.Body.Close()
		if err != nil {
			log.Error("Failed to close response body", zap.Error(err))
		}
	}()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Error("Failed to read the response body", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve resource")
	}
	return body, nil
}

// Get A function to enable communication with attribution service via GET
func (ac *AttributionClient) Get(ctx context.Context, resourceType ResourceType, id string) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	url := fmt.Sprintf("%s/%s/%s", ac.config.URL, resourceType, id)
	return ac.doGet(ctx, url)
}

func (ac *AttributionClient) doGet(ctx context.Context, url string) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	req, err := retryablehttp.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve resource %s", url)
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	if ctx.Value(middleware2.ContextKeyOrganization) != nil {
		req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	}
	resp, err := ac.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve resource %s", url)
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return nil, errors.Errorf("Failed to retrieve resource %s", url)
	}

	defer func() {
		err := resp.Body.Close()
		if err != nil {
			log.Error("Failed to close response body", zap.Error(err))
		}
	}()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Error("Failed to read the response body", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve resource %s", url)
	}
	return body, nil
}

// Export A function to enable starting a data export job via GET
func (ac *AttributionClient) Export(ctx context.Context, resourceType ResourceType, id string) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	url := generateURL(ctx, ac.config.URL, resourceType, id)
	req, err := retryablehttp.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to export data for %s", resourceType)
	}

	req = setExportRequestHeaders(ctx, req)

	resp, err := ac.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to start job for %s/%s", resourceType, id)
	}

	defer func() {
		err := resp.Body.Close()
		if err != nil {
			log.Error("Failed to close response body", zap.Error(err))
		}
	}()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Error("Failed to read the response body", zap.Error(err))
		return nil, errors.Errorf("Failed to start job for %s/%s", resourceType, id)
	}
	errMsg := checkForErrorMsg(body)
	if errMsg != "" {
		return nil, errors.Errorf(errMsg)
	}
	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return nil, errors.Errorf("Failed to start job for %s/%s", resourceType, id)
	}
	return body, nil
}

func checkForErrorMsg(body []byte) string {
	var br struct {
		Message string `json:"message"`
	}
	_ = json.Unmarshal(body, &br)
	return br.Message
}

func setExportRequestHeaders(ctx context.Context, req *retryablehttp.Request) *retryablehttp.Request {
	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	if ctx.Value(middleware2.ContextKeyRequestingIP) != nil {
		req.Header.Add(middleware2.FwdHeader, ctx.Value(middleware2.ContextKeyRequestingIP).(string))
	}
	if ctx.Value(middleware2.ContextKeyOrganization) != nil {
		req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	}
	if ctx.Value(middleware2.ContextKeyRequestURL) != nil {
		req.Header.Add(middleware2.RequestURLHeader, ctx.Value(middleware2.ContextKeyRequestURL).(string))
	}
	return req
}

func generateURL(ctx context.Context, baseURL string, resource ResourceType, id string) string {
	params := fmt.Sprintf("?_type=%s", ctx.Value(middleware2.ContextKeyResourceTypes))
	if ctx.Value(middleware2.ContextKeySince) != "" {
		params = fmt.Sprintf("%s&_since=%s", params, ctx.Value(middleware2.ContextKeySince))
	}
	return fmt.Sprintf("%s/%s/%s/$export%s", baseURL, resource, id, params)
}

// Post A function to enable communication with attribution service via Post
func (ac *AttributionClient) Post(ctx context.Context, resourceType ResourceType, body []byte) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	url := fmt.Sprintf("%s/%s", ac.config.URL, resourceType)
	req, err := retryablehttp.NewRequest(http.MethodPost, url, body)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to save resource %s", resourceType)
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	if ctx.Value(middleware2.ContextKeyOrganization) != nil {
		req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	}
	resp, err := ac.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to save resource %s", resourceType)
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return nil, errors.Errorf("Failed to save resource %s", resourceType)
	}

	defer func() {
		err := resp.Body.Close()
		if err != nil {
			log.Error("Failed to close response body", zap.Error(err))
		}
	}()

	b, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Error("Failed to read the response body", zap.Error(err))
		return nil, errors.Errorf("Failed to save resource %s", resourceType)
	}
	return b, nil
}

// Delete A function to enable communication with attribution service via DELETE
func (ac *AttributionClient) Delete(ctx context.Context, resourceType ResourceType, id string) error {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	url := fmt.Sprintf("%s/%s/%s", ac.config.URL, resourceType, id)
	req, err := retryablehttp.NewRequest(http.MethodDelete, url, nil)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return errors.Errorf("Failed to delete resource %s/%s", resourceType, id)
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	if ctx.Value(middleware2.ContextKeyOrganization) != nil {
		req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	}
	resp, err := ac.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return errors.Errorf("Failed to delete resource %s/%s", resourceType, id)
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return errors.Errorf("Failed to delete resource %s/%s", resourceType, id)
	}

	defer func() {
		err := resp.Body.Close()
		if err != nil {
			log.Error("Failed to close response body", zap.Error(err))
		}
	}()

	return nil
}

// Put A function to enable communication with attribution service via Put
func (ac *AttributionClient) Put(ctx context.Context, resourceType ResourceType, id string, body []byte) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	url := fmt.Sprintf("%s/%s/%s", ac.config.URL, resourceType, id)
	return ac.doPut(ctx, url, body)
}

// UpdateImplOrg function to update a specific implementer/org relation
func (ac *AttributionClient) UpdateImplOrg(ctx context.Context, implID string, orgID string, rel ImplementerOrg) (ImplementerOrg, error) {
	log := logger.WithContext(ctx)
	reqBytes := new(bytes.Buffer)
	if err := json.NewEncoder(reqBytes).Encode(rel); err != nil {
		log.Error(fmt.Sprintf("Failed to convert model to bytes, ImplID: %s OrgID: %s", implID, orgID), zap.Error(err))
		return ImplementerOrg{}, errors.Errorf("Failed to update implementerOrg relation")
	}
	url := fmt.Sprintf("%s/Implementer/%s/org/%s", ac.config.URL, implID, orgID)

	resBytes, err := ac.doPut(ctx, url, reqBytes.Bytes())
	if err != nil {
		log.Error(fmt.Sprintf("Update ImplementerOrg request failed, ImplID: %s OrgID: %s", implID, orgID), zap.Error(err))
		return ImplementerOrg{}, errors.Errorf("Failed to update implementerOrg relation")
	}
	resp := ImplementerOrg{}
	if err := json.NewDecoder(bytes.NewReader(resBytes)).Decode(&resp); err != nil {
		log.Error(fmt.Sprintf("Failed to convert bytes to ImplementerOrg model during update, ImplID: %s OrgID: %s", implID, orgID), zap.Error(err))
		return ImplementerOrg{}, errors.Errorf("Failed to update implementerOrg relation")
	}
	return resp, nil
}

// GetProviderOrgs function to retrieve lists or orgs managed by an implementer
func (ac *AttributionClient) GetProviderOrgs(ctx context.Context, implID string) ([]ProviderOrg, error) {
	log := logger.WithContext(ctx)

	url := fmt.Sprintf("%s/Implementer/%s/org", ac.config.URL, implID)

	resBytes, err := ac.doGet(ctx, url)
	if err != nil {
		log.Error(fmt.Sprintf("Get implementerOrg relation failed, ImplID: %s", implID), zap.Error(err))
		return []ProviderOrg{}, errors.Errorf("Failed to update implementerOrg relation")
	}
	resp := []ProviderOrg{}
	if err := json.NewDecoder(bytes.NewReader(resBytes)).Decode(&resp); err != nil {
		log.Error(fmt.Sprintf("Failed to convert bytes to ImplementerOrg model, ImplID: %s", implID), zap.Error(err))
		return []ProviderOrg{}, errors.Errorf("Failed to get implementerOrg relation")
	}
	return resp, nil
}

func (ac *AttributionClient) doPut(ctx context.Context, url string, body []byte) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	req, err := retryablehttp.NewRequest(http.MethodPut, url, body)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to update resource")
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	if ctx.Value(middleware2.ContextKeyOrganization) != nil {
		req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	}
	resp, err := ac.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to update resource")
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return nil, errors.Errorf("Failed to update resource")
	}

	defer func() {
		err := resp.Body.Close()
		if err != nil {
			log.Error("Failed to close response body", zap.Error(err))
		}
	}()

	b, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Error("Failed to read the response body", zap.Error(err))
		return nil, errors.Errorf("Failed to update resource")
	}
	return b, nil
}
