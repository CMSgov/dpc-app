package client

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/logger"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/model"
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
	Export(ctx context.Context, request model.ExportRequest) ([]byte, error)
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

// Export function to export data for patients
func (jc *JobClientImpl) Export(ctx context.Context, request model.ExportRequest) ([]byte, error) {
	log := logger.WithContext(ctx)
	jc.httpClient.Logger = newLogger(*log)

	requestBytes, err := json.Marshal(request)
	if err != nil {
		log.Error("Failed to convert request into bytes", zap.Error(err))
		return nil, errors.Wrap(err, "Failed to export")
	}

	url := fmt.Sprintf("%s/Job", jc.config.URL)
	req, err := retryablehttp.NewRequest(http.MethodPost, url, requestBytes)
	if err != nil {
		log.Error("Failed to create request", zap.Error(err))
		return nil, errors.Wrap(err, "Failed to export")
	}

	req.Header.Add(middleware.RequestIDHeader, ctx.Value(middleware.RequestIDKey).(string))

	setExportRequestHeaders(ctx, req)

	resp, err := jc.httpClient.Do(req)
	if err != nil {
		log.Error("Failed to send request", zap.Error(err))
		return nil, errors.Wrap(err, "Failed to export")
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
		return nil, errors.Wrap(err, "Failed to export")
	}

	errMsg := checkForErrorMsg(body)
	if errMsg != "" {
		return nil, errors.Errorf(errMsg)
	}
	if resp.StatusCode < 200 || resp.StatusCode > 299 {
		return nil, errors.Errorf("Failed to export")
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
