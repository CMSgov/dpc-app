package client

import (
	"context"
	"fmt"
	"github.com/CMSgov/dpc/api/logger"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/go-chi/chi/middleware"
	"github.com/pkg/errors"
	"go.uber.org/zap"
	"io/ioutil"
	"net/http"

	"github.com/hashicorp/go-retryablehttp"
)

// JobConfig is a struct to hold configuration info for retryablehttp client
type JobConfig struct {
	URL     string
	Retries int
}

// JobClient interface for testing purposes
type JobClient interface {
	Status(ctx context.Context, jobID string) ([]byte, error)
}

// JobClientImpl is a struct to hold the retryablehttp client and configs
type JobClientImpl struct {
	config     JobConfig
	httpClient *retryablehttp.Client
}

// NewJobClient initializes the retryable client and returns a reference to the attribution client
func NewJobClient(config JobConfig) JobClient {
	client := retryablehttp.NewClient()
	client.RetryMax = config.Retries
	client.Logger = newLogger(*logger.WithContext(context.Background()))
	return &JobClientImpl{
		config:     config,
		httpClient: client,
	}
}

// Status function to get status from job service
func (jc *JobClientImpl) Status(ctx context.Context, jobID string) ([]byte, error) {
	log := logger.WithContext(ctx)
	jc.httpClient.Logger = newLogger(*log)

	url := fmt.Sprintf("%s/%s/%s", jc.config.URL, "Job", jobID)
	req, err := retryablehttp.NewRequest(http.MethodGet, url, nil)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve status for job %s", jobID)
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))
	if ctx.Value(middleware2.ContextKeyOrganization) != nil {
		req.Header.Add(middleware2.OrgHeader, ctx.Value(middleware2.ContextKeyOrganization).(string))
	}
	resp, err := jc.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Errorf("Failed to retrieve status for job %s", jobID)
	}

	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return nil, errors.Errorf("Failed to retrieve status for job %s", jobID)
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
		return nil, errors.Errorf("Failed to retrieve status for job %s", jobID)
	}
	return body, nil
}
