package v2

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/model"
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
	mjc *MockJobClient
}

func (suite *GroupControllerTestSuite) SetupTest() {
	mac := new(MockAttributionClient)
	mjc := new(MockJobClient)
	suite.mac = mac
	suite.mjc = mjc
	suite.grp = NewGroupController(mac, mjc)

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
}

func (suite *GroupControllerTestSuite) TestCreateMalformedGroup() {
	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", bytes.NewReader(apitest.MalformedOrg()))

	w := httptest.NewRecorder()
	suite.grp.Create(w, req)
	res := w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)
}

func (suite *GroupControllerTestSuite) TestExportGroup() {
	jobID := "test-export-job"
	ab := apitest.AttributionToFHIRResponse(apitest.FilteredGroupjson)
	var r model.Resource
	_ = json.Unmarshal(ab, &r)

	suite.mjc.On("Export", mock.Anything, mock.Anything).Return([]byte(jobID), nil)
	suite.mac.On("Get", mock.Anything, mock.Anything, mock.Anything).Return(ab, nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodGet, "http://example.com/Group/9876/$export?_outputFormat=application/fhir%2Bndjson", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, r.ID)
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, middleware2.ContextKeyResourceTypes, middleware2.AllResources)
	req = req.WithContext(ctx)
	req.Header.Set("Prefer", "respond-async")

	w := httptest.NewRecorder()
	suite.grp.Export(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusAccepted, res.StatusCode)
	assert.Equal(suite.T(), fmt.Sprintf("http://example.com/v2/Jobs/%s", jobID), res.Header.Get("Content-Location"))

	resp, _ := ioutil.ReadAll(res.Body)
	ja.Assertf(string(resp), "")
}

func (suite *GroupControllerTestSuite) TestExportGroupMissingPreferHeader() {
	ab := apitest.AttributionToFHIRResponse(apitest.FilteredGroupjson)
	var r model.Resource
	_ = json.Unmarshal(ab, &r)

	suite.mjc.On("Export", mock.Anything, mock.Anything).Return(apitest.AttributionResponse(apitest.JobJSON), nil)
	suite.mac.On("Get", mock.Anything, mock.Anything, mock.Anything).Return(ab, nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodGet, "http://example.com/Group/9876/$export?_outputFormat=application/fhir%2Bndjson", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, r.ID)
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyResourceTypes, middleware2.AllResources)
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
	ab := apitest.AttributionToFHIRResponse(apitest.FilteredGroupjson)
	var r model.Resource
	_ = json.Unmarshal(ab, &r)

	suite.mjc.On("Export", mock.Anything, mock.Anything).Return(apitest.AttributionResponse(apitest.JobJSON), nil)
	suite.mac.On("Get", mock.Anything, mock.Anything, mock.Anything).Return(ab, nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodGet, "http://example.com/Group/9876/$export?_outputFormat=application/fhir%2Bndjson", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, r.ID)
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyResourceTypes, middleware2.AllResources)
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
	ab := apitest.AttributionToFHIRResponse(apitest.FilteredGroupjson)
	var r model.Resource
	_ = json.Unmarshal(ab, &r)

	suite.mjc.On("Export", mock.Anything, mock.Anything).Return(apitest.AttributionResponse(apitest.JobJSON), nil)
	suite.mac.On("Get", mock.Anything, mock.Anything, mock.Anything).Return(ab, nil)

	req := httptest.NewRequest(http.MethodGet, "http://example.com/Group/9876/$export", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, r.ID)
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyResourceTypes, middleware2.AllResources)
	req = req.WithContext(ctx)
	req.Header.Set("Prefer", "respond-async")

	w := httptest.NewRecorder()
	suite.grp.Export(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusAccepted, res.StatusCode)
}

func (suite *GroupControllerTestSuite) TestExportGroupInvalidOutputFormat() {
	ab := apitest.AttributionToFHIRResponse(apitest.FilteredGroupjson)
	var r model.Resource
	_ = json.Unmarshal(ab, &r)

	suite.mjc.On("Export", mock.Anything, mock.Anything).Return(apitest.AttributionResponse(apitest.JobJSON), nil)
	suite.mac.On("Get", mock.Anything, mock.Anything, mock.Anything).Return(ab, nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodGet, "http://example.com/Group/9876/$export?_outputFormat=INVALID", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, r.ID)
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyResourceTypes, middleware2.AllResources)
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
