package client

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/model"
	"io/ioutil"
	"net/http"

	"github.com/CMSgov/dpc/api/logger"
	"github.com/hashicorp/go-retryablehttp"
	"github.com/pkg/errors"
	"go.uber.org/zap"
)

// SsasHTTPClientConfig is a struct to hold configuration info for retryable http client
type SsasHTTPClientConfig struct {
	URL          string
	Retries      int
	ClientID     string
	ClientSecret string
}

// Contains the different ResourceType for calls to attribution
const (
	PostV2SystemEndpoint string = "v2/system"
	PostV2GroupEndpoint  string = "v2/group"
	V2KeyEndpoint        string = "key"
)

// SsasClient interface for testing purposes
type SsasClient interface {
	CreateSystem(ctx context.Context, request CreateSystemRequest) (CreateSystemResponse, error)
	CreateGroup(ctx context.Context, request CreateGroupRequest) (CreateGroupResponse, error)
	GetSystem(ctx context.Context, systemID string) (GetSystemResponse, error)
	AddPublicKey(ctx context.Context, systemID string, request model.ProxyPublicKeyRequest) (map[string]string, error)
	DeletePublicKey(ctx context.Context, systemID string, keyID string) error
}

// SsasHTTPClient is a struct to hold the retryable http client and configs
type SsasHTTPClient struct {
	config     SsasHTTPClientConfig
	httpClient *retryablehttp.Client
}

// NewSsasHTTPClient initializes the retryable client and returns a reference to the ssas client
func NewSsasHTTPClient(config SsasHTTPClientConfig) SsasClient {
	client := retryablehttp.NewClient()
	client.RetryMax = config.Retries
	return &SsasHTTPClient{
		config:     config,
		httpClient: client,
	}
}

// GetSystem function to get a ssas system
func (sc *SsasHTTPClient) GetSystem(ctx context.Context, systemID string) (GetSystemResponse, error) {
	log := logger.WithContext(ctx)

	url := fmt.Sprintf("%s/%s/%s", sc.config.URL, PostV2SystemEndpoint, systemID)

	resBytes, err := sc.doGet(ctx, url)
	if err != nil {
		log.Error("Get ssas system request failed", zap.Error(err))
		return GetSystemResponse{}, err
	}
	resp := GetSystemResponse{}
	if err := json.NewDecoder(bytes.NewReader(resBytes)).Decode(&resp); err != nil {
		log.Error("Failed to convert SSAS response bytes to GetSystemResponse model", zap.Error(err))
		return GetSystemResponse{}, err
	}
	return resp, nil
}

// DeletePublicKey function to delete a public key from ssas system
func (sc *SsasHTTPClient) DeletePublicKey(ctx context.Context, systemID string, keyID string) error {
	log := logger.WithContext(ctx)

	url := fmt.Sprintf("%s/%s/%s/%s/%s", sc.config.URL, PostV2SystemEndpoint, systemID, V2KeyEndpoint, keyID)

	err := sc.doDelete(ctx, url)
	if err != nil {
		log.Error("Delete public key failed", zap.Error(err))
		return err
	}
	return nil
}

// AddPublicKey function to add a public key to ssas system
func (sc *SsasHTTPClient) AddPublicKey(ctx context.Context, systemID string, request model.ProxyPublicKeyRequest) (map[string]string, error) {
	log := logger.WithContext(ctx)
	reqBytes := new(bytes.Buffer)
	if err := json.NewEncoder(reqBytes).Encode(request); err != nil {
		log.Error("Failed to convert model to bytes", zap.Error(err))
		return nil, err
	}
	url := fmt.Sprintf("%s/%s/%s/%s", sc.config.URL, PostV2SystemEndpoint, systemID, V2KeyEndpoint)

	resBytes, err := sc.doPost(ctx, url, reqBytes.Bytes())
	if err != nil {
		log.Error("Add public key failed", zap.Error(err))
		return nil, err
	}
	var resp map[string]string
	if err := json.NewDecoder(bytes.NewReader(resBytes)).Decode(&resp); err != nil {
		log.Error("Failed to convert ssas response bytes to map model", zap.Error(err))
		return nil, err
	}
	return resp, nil
}

// CreateSystem function to create a new ssas system
func (sc *SsasHTTPClient) CreateSystem(ctx context.Context, request CreateSystemRequest) (CreateSystemResponse, error) {
	log := logger.WithContext(ctx)
	reqBytes := new(bytes.Buffer)
	if err := json.NewEncoder(reqBytes).Encode(request); err != nil {
		log.Error("Failed to convert model to bytes", zap.Error(err))
		return CreateSystemResponse{}, errors.Errorf("Failed to create ssas system")
	}
	url := fmt.Sprintf("%s/%s", sc.config.URL, PostV2SystemEndpoint)

	resBytes, err := sc.doPost(ctx, url, reqBytes.Bytes())
	if err != nil {
		log.Error("Create ssas system request failed", zap.Error(err))
		return CreateSystemResponse{}, errors.Errorf("Failed to create ssas system")
	}
	resp := CreateSystemResponse{}
	if err := json.NewDecoder(bytes.NewReader(resBytes)).Decode(&resp); err != nil {
		log.Error("Failed to convert ssas response bytes to CreateGroupResponse model", zap.Error(err))
		return CreateSystemResponse{}, errors.Errorf("Failed to create ssas system")
	}
	return resp, nil
}

// CreateGroup function to create a new ssas group
func (sc *SsasHTTPClient) CreateGroup(ctx context.Context, request CreateGroupRequest) (CreateGroupResponse, error) {
	log := logger.WithContext(ctx)
	reqBytes := new(bytes.Buffer)
	if err := json.NewEncoder(reqBytes).Encode(request); err != nil {
		log.Error("Failed to convert model to bytes", zap.Error(err))
		return CreateGroupResponse{}, errors.Errorf("Failed to create ssas group")
	}
	url := fmt.Sprintf("%s/%s", sc.config.URL, PostV2GroupEndpoint)

	resBytes, err := sc.doPost(ctx, url, reqBytes.Bytes())
	if err != nil {
		log.Error("Create ssas group request failed", zap.Error(err))
		return CreateGroupResponse{}, errors.Errorf("Failed to create ssas group")
	}
	resp := CreateGroupResponse{}
	if err := json.NewDecoder(bytes.NewReader(resBytes)).Decode(&resp); err != nil {
		log.Error("Failed to convert ssas response bytes to CreateGroupResponse model", zap.Error(err))
		return CreateGroupResponse{}, errors.Errorf("Failed to create ssas group")
	}
	return resp, nil
}

func (sc *SsasHTTPClient) doPost(ctx context.Context, url string, reqBytes []byte) ([]byte, error) {
	log := logger.WithContext(ctx)
	sc.httpClient.Logger = newLogger(*log)

	req, err := retryablehttp.NewRequest(http.MethodPost, url, reqBytes)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to create ssas group")
	}
	req.SetBasicAuth(sc.config.ClientID, sc.config.ClientSecret)
	resp, err := sc.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to create ssas group")
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		b, _ := ioutil.ReadAll(resp.Body)
		body := string(b[:])
		return nil, errors.Errorf(body)
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
		return nil, errors.Errorf("Failed to save system")
	}
	return b, nil
}

func (sc *SsasHTTPClient) doGet(ctx context.Context, url string) ([]byte, error) {
	log := logger.WithContext(ctx)
	sc.httpClient.Logger = newLogger(*log)

	req, err := retryablehttp.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, err
	}
	req.SetBasicAuth(sc.config.ClientID, sc.config.ClientSecret)
	resp, err := sc.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, err
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		b, _ := ioutil.ReadAll(resp.Body)
		body := string(b[:])
		return nil, errors.Errorf(body)
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
		return nil, err
	}
	return b, nil
}

func (sc *SsasHTTPClient) doDelete(ctx context.Context, url string) error {
	log := logger.WithContext(ctx)
	sc.httpClient.Logger = newLogger(*log)

	req, err := retryablehttp.NewRequest(http.MethodDelete, url, nil)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return errors.Errorf("Failed to delete resource")
	}
	req.SetBasicAuth(sc.config.ClientID, sc.config.ClientSecret)
	resp, err := sc.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return errors.Errorf("Failed to delete resource")
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		b, _ := ioutil.ReadAll(resp.Body)
		body := string(b[:])
		return errors.Errorf(body)
	}

	defer func() {
		err := resp.Body.Close()
		if err != nil {
			log.Error("Failed to close response body", zap.Error(err))
		}
	}()

	return nil
}

// CreateGroupRequest struct to model a ssas request to create a group
type CreateGroupRequest struct {
	Name    string `json:"name"`
	GroupID string `json:"group_id"`
	XData   string `json:"xdata"`
}

// CreateGroupResponse struct to model a ssas response to create a group
type CreateGroupResponse struct {
	ID      int    `json:"id" faker:"oneof: 15, 27, 61, 42, 100"`
	GroupID string `json:"group_id" faker:"uuid_hyphenated"`
}

// CreateSystemRequest struct to model a ssas request to create a system
type CreateSystemRequest struct {
	ClientName string   `json:"client_name"`
	GroupID    string   `json:"group_id"`
	Scope      string   `json:"scope"`
	PublicKey  string   `json:"public_key"`
	IPs        []string `json:"ips"`
	XData      string   `json:"xdata,omitempty"`
}

// CreateSystemResponse struct to model a ssas response to create a system
type CreateSystemResponse struct {
	SystemID    string   `json:"system_id"`
	ClientName  string   `json:"client_name"`
	GroupID     string   `json:"group_id"`
	Scope       string   `json:"scope"`
	PublicKey   string   `json:"public_key"`
	IPs         []string `json:"ips"`
	TrackingID  string   `json:"tracking_id"`
	ClientID    string   `json:"client_id"`
	ClientToken string   `json:"client_token"`
	ExpiresAt   string   `json:"expires_at"`
	XData       string   `json:"xdata,omitempty"`
}

// GetSystemResponse struct to model a ssas response to get a system
type GetSystemResponse struct {
	GID          string              `json:"g_id"`
	GroupID      string              `json:"group_id"`
	ClientID     string              `json:"client_id"`
	SoftwareID   string              `json:"software_id"`
	ClientName   string              `json:"client_name"`
	APIScope     string              `json:"api_scope"`
	XData        string              `json:"x_data"`
	LastTokenAt  string              `json:"last_token_at"`
	PublicKeys   []map[string]string `json:"public_keys"`
	IPs          []map[string]string `json:"ips"`
	ClientTokens []map[string]string `json:"client_tokens"`
}
