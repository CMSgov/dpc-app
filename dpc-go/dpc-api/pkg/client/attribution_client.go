package client

import (
	"fmt"
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

func (attributionClient *AttributionClient) Get(resourceType ResourceType, id string) ([]byte, error) {
	url := fmt.Sprintf("%s/%s/%s", attributionClient.config.URL, resourceType, id)
	req, err := retryablehttp.NewRequest("GET", url, nil)
	if err != nil {
		zap.L().Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve resource %s/%s", resourceType, id)
	}

	resp, err := attributionClient.httpClient.Do(req)
	if err != nil {
		zap.L().Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve resource %s/%s", resourceType, id)
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return nil, errors.Errorf("Failed to retrieve resource %s/%s", resourceType, id)
	}

	defer func() {
		err := resp.Body.Close()
		if err != nil {
			zap.L().Error("Failed to close response body", zap.Error(err))
		}
	}()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		zap.L().Error("Failed to read the response body", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve resource %s/%s", resourceType, id)
	}
	return body, nil
}

func (attributionClient *AttributionClient) Post(resourceType ResourceType, organization []byte) ([]byte, error) {
	url := fmt.Sprintf("%s/%s", attributionClient.config.URL, resourceType)
	req, err := retryablehttp.NewRequest("POST", url, organization)
	if err != nil {
		zap.L().Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to save resource %s", resourceType)
	}

	resp, err := attributionClient.httpClient.Do(req)
	if err != nil {
		zap.L().Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to save resource %s", resourceType)
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return nil, errors.Errorf("Failed to save resource %s", resourceType)
	}

	defer func() {
		err := resp.Body.Close()
		if err != nil {
			zap.L().Error("Failed to close response body", zap.Error(err))
		}
	}()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		zap.L().Error("Failed to read the response body", zap.Error(err))
		return nil, errors.Errorf("Failed to save resource %s", resourceType)
	}
	return body, nil
}
