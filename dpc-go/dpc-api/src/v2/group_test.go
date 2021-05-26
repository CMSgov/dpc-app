package v2

import (
	"bytes"
	"context"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/CMSgov/dpc/api/apitest"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/bxcodec/faker/v3"
	"github.com/go-chi/chi/middleware"
	"github.com/kinbiko/jsonassert"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type GroupControllerTestSuite struct {
	suite.Suite
	grp *GroupController
	mac *MockAttributionClient
}

func (suite *GroupControllerTestSuite) SetupTest() {
	mac := new(MockAttributionClient)
	suite.mac = mac
	suite.grp = NewGroupController(mac)

}

func TestGroupControllerTestSuite(t *testing.T) {
	suite.Run(t, new(GroupControllerTestSuite))
}

func (suite *GroupControllerTestSuite) TestCreateGroupErrorInClient() {
	suite.mac.On("Post", mock.Anything, mock.Anything, mock.Anything).Return(make([]byte, 0), errors.New("Test Error"))

	ja := jsonassert.New(suite.T())

	req := httptest.NewRequest(http.MethodPost, "http://example.com/Group", strings.NewReader(apitest.Groupjson))
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.grp.Create(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusUnprocessableEntity, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
        "issue": [
            {
                "severity": "error",
                "code": "Exception",
                "details": {
                    "text": "Failed to save group"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)

}

func (suite *GroupControllerTestSuite) TestCreateGroupBadJson() {
	ja := jsonassert.New(suite.T())

	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.grp.Create(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
        "issue": [
            {
                "severity": "warning",
                "code": "Business Rule Violation",
                "details": {
                    "text": "Not a valid group"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)
}

func (suite *GroupControllerTestSuite) TestCreateGroup() {
	suite.mac.On("Post", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionToFHIRResponse(apitest.FilteredGroupjson), nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", strings.NewReader(apitest.FilteredGroupjson))

	w := httptest.NewRecorder()
	suite.grp.Create(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)
	ja.Assertf(string(resp), string(apitest.AttributionToFHIRResponse(apitest.FilteredGroupjson)))

	req = httptest.NewRequest(http.MethodPost, "http://example.com/foo", bytes.NewReader(apitest.MalformedOrg()))

	w = httptest.NewRecorder()
	suite.grp.Create(w, req)
	res = w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)
}

func (suite *GroupControllerTestSuite) TestExportGroup() {
	suite.mac.On("Export", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionResponse(apitest.JobJSON), nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodGet, "http://example.com/Group/9876/$export?_outputFormat=application/fhir%2Bndjson", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, faker.UUIDHyphenated())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	req = req.WithContext(ctx)
	req.Header.Set("Prefer", "respond-async")

	w := httptest.NewRecorder()
	suite.grp.Export(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusAccepted, res.StatusCode)
	assert.Equal(suite.T(), "http://example.com/v2/Jobs/test-export-job", res.Header.Get("Content-Location"))

	resp, _ := ioutil.ReadAll(res.Body)
	ja.Assertf(string(resp), "")
}

func (suite *GroupControllerTestSuite) TestExportGroupMissingPreferHeader() {
	suite.mac.On("Export", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionResponse(apitest.JobJSON), nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodGet, "http://example.com/Group/9876/$export?_outputFormat=application/fhir%2Bndjson", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, faker.UUIDHyphenated())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()
	suite.grp.Export(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
        "issue": [
            {
                "severity": "warning",
                "code": "Business Rule Violation",
                "details": {
                    "text": "The 'Prefer' header is required and must be 'respond-async'"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)
}

func (suite *GroupControllerTestSuite) TestExportGroupInvalidPreferHeader() {
	suite.mac.On("Export", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionResponse(apitest.JobJSON), nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodGet, "http://example.com/Group/9876/$export?_outputFormat=application/fhir%2Bndjson", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, faker.UUIDHyphenated())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)
	req.Header.Set("Prefer", "INVALID")

	w := httptest.NewRecorder()
	suite.grp.Export(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
        "issue": [
            {
                "severity": "warning",
                "code": "Business Rule Violation",
                "details": {
                    "text": "The 'Prefer' header must be 'respond-async'"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)
}

func (suite *GroupControllerTestSuite) TestExportGroupMissingOutputFormat() {
	suite.mac.On("Export", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionResponse(apitest.JobJSON), nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodGet, "http://example.com/Group/9876/$export", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, faker.UUIDHyphenated())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)
	req.Header.Set("Prefer", "respond-async")

	w := httptest.NewRecorder()
	suite.grp.Export(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
        "issue": [
            {
                "severity": "warning",
                "code": "Business Rule Violation",
                "details": {
                    "text": "'_outputFormat' query parameter must be 'application/fhir+ndjson', 'application/ndjson', or 'ndjson'"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)
}

func (suite *GroupControllerTestSuite) TestExportGroupInvalidOutputFormat() {
	suite.mac.On("Export", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionResponse(apitest.JobJSON), nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodGet, "http://example.com/Group/9876/$export?_outputFormat=INVALID", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, faker.UUIDHyphenated())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)
	req.Header.Set("Prefer", "respond-async")

	w := httptest.NewRecorder()
	suite.grp.Export(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
        "issue": [
            {
                "severity": "warning",
                "code": "Business Rule Violation",
                "details": {
                    "text": "'_outputFormat' query parameter must be 'application/fhir+ndjson', 'application/ndjson', or 'ndjson'"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)
}

func (suite *GroupControllerTestSuite) TestReadNotImplemented() {
	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.grp.Read(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}

func (suite *GroupControllerTestSuite) TestDeleteNotImplemented() {
	req := httptest.NewRequest(http.MethodDelete, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.grp.Delete(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}

func (suite *GroupControllerTestSuite) TestUpdateNotImplemented() {
	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.grp.Update(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}
