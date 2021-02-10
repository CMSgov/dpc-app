package client

import (
	"context"
	"fmt"
	"github.com/CMSgov/dpc/api/logger"
	"github.com/go-chi/chi/middleware"
	"github.com/hashicorp/go-retryablehttp"
	"github.com/pkg/errors"
	"go.uber.org/zap"
	"io/ioutil"
)

type AttributionConfig struct {
	URL     string
	Retries int
}

// ResourceType of request
type ResourceType string

const (
	Organization ResourceType = "Organization"
)

// AttributionClient to make calls to Attribution service
type AttributionClient struct {
	config     *AttributionConfig
	httpClient *retryablehttp.Client
}

// NewAttribution creates new Attribution client
func NewAttributionClient(config *AttributionConfig) *AttributionClient {
	client := retryablehttp.NewClient()
	client.RetryMax = config.Retries
	return &AttributionClient{
		config:     config,
		httpClient: client,
	}
}

func (ac *AttributionClient) Get(ctx context.Context, resourceType ResourceType, id string) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	url := fmt.Sprintf("%s/%s/%s", ac.config.URL, resourceType, id)
	req, err := retryablehttp.NewRequest("GET", url, nil)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve resource %s/%s", resourceType, id)
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
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

func (ac *AttributionClient) Post(ctx context.Context, resourceType ResourceType, organization []byte) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	url := fmt.Sprintf("%s/%s", ac.config.URL, resourceType)
	req, err := retryablehttp.NewRequest("POST", url, organization)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to save resource %s", resourceType)
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
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

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		log.Error("Failed to read the response body", zap.Error(err))
		return nil, errors.Errorf("Failed to save resource %s", resourceType)
	}
	return body, nil
}
