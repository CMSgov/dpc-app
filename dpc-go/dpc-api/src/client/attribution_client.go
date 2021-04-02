package client

import (
	"context"
	"fmt"
	"io/ioutil"
	"net/http"

	"github.com/CMSgov/dpc/api/logger"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/go-chi/chi/middleware"

	// "github.com/CMSgov/dpc/api/middleware"

	"github.com/hashicorp/go-retryablehttp"
	"github.com/pkg/errors"
	"go.uber.org/zap"
)

// AttributionConfig is a struct to hold configuration info for retryablehttp client
type AttributionConfig struct {
	URL     string
	Retries int
}

// ResourceType is a type to be used when making requests to different endpoints in attribution service
type ResourceType string

// Contains the different ResourceType for calls to attribution
const (
	Organization ResourceType = "Organization"
	Group        ResourceType = "Group"
)

// Client interface for testing purposes
type Client interface {
	Get(ctx context.Context, resourceType ResourceType, id string) ([]byte, error)
	Post(ctx context.Context, resourceType ResourceType, body []byte) ([]byte, error)
	Delete(ctx context.Context, resourceType ResourceType, id string) error
	Put(ctx context.Context, resourceType ResourceType, id string, body []byte) ([]byte, error)
}

// AttributionClient is a struct to hold the retryablehttp client and configs
type AttributionClient struct {
	config     AttributionConfig
	httpClient *retryablehttp.Client
}

// NewAttributionClient initializes the retryable client and returns a reference to the attribution cleint
func NewAttributionClient(config AttributionConfig) *AttributionClient {
	client := retryablehttp.NewClient()
	client.RetryMax = config.Retries
	return &AttributionClient{
		config:     config,
		httpClient: client,
	}
}

// Get A function to enable communication with attribution service via GET
func (ac *AttributionClient) Get(ctx context.Context, resourceType ResourceType, id string) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	url := fmt.Sprintf("%s/%s/%s", ac.config.URL, resourceType, id)
	req, err := retryablehttp.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve resource %s/%s", resourceType, id)
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	resp, err := ac.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve resource %s/%s", resourceType, id)
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return nil, errors.Errorf("Failed to retrieve resource %s/%s", resourceType, id)
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
		return nil, errors.Errorf("Failed to retrieve resource %s/%s", resourceType, id)
	}
	return body, nil
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
	req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
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
	req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
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
	req, err := retryablehttp.NewRequest(http.MethodPut, url, body)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to update resource %s", resourceType)
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	resp, err := ac.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to update resource %s", resourceType)
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return nil, errors.Errorf("Failed to update resource %s", resourceType)
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
		return nil, errors.Errorf("Failed to update resource %s", resourceType)
	}
	return b, nil
}
