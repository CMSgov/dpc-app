package client

import (
	"context"
	"fmt"
	"github.com/CMSgov/dpc/api/logger"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/go-chi/chi/middleware"
	"github.com/hashicorp/go-retryablehttp"
	"github.com/pkg/errors"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"
)

// DataConfig is a struct to hold configuration info for retryablehttp client
type DataConfig struct {
	URL     string
	Retries int
}

// DataClient interface for testing purposes
type DataClient interface {
	Data(ctx context.Context, path string) ([]byte, error)
}

// DataClientImpl is a struct to hold the retryablehttp client and configs
type DataClientImpl struct {
	config     DataConfig
	httpClient *retryablehttp.Client
}

// NewDataClient initializes the retryable client and returns a reference to the data client
func NewDataClient(config DataConfig) DataClient {
	client := retryablehttp.NewClient()
	client.RetryMax = config.Retries
	return &DataClientImpl{
		config:     config,
		httpClient: client,
	}
}

// Data A function to enable communication with attribution service via the Data route
func (ac *DataClientImpl) Data(ctx context.Context, path string) ([]byte, error) {
	log := logger.WithContext(ctx)
	ac.httpClient.Logger = newLogger(*log)

	url := fmt.Sprintf("%s/Data/%s", ac.config.URL, path)
	req, err := retryablehttp.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to get data info %s", path)
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	if ctx.Value(middleware2.ContextKeyOrganization) != nil {
		req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	}
	resp, err := ac.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to get data info %s", path)
	}

	if resp.StatusCode != 200 {
		return nil, errors.Errorf("Failed to get data info %s", path)
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
		return nil, errors.Errorf("Failed to retrieve data")
	}
	return body, nil

}
