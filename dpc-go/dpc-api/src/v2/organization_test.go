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
	"github.com/CMSgov/dpc/api/client"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/go-chi/chi/middleware"
	"github.com/kinbiko/jsonassert"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type MockAttributionClient struct {
	mock.Mock
}

func (ac *MockAttributionClient) Export(ctx context.Context, resourceType client.ResourceType, id string) ([]byte, error) {
	args := ac.Called(ctx, resourceType, id)
	return args.Get(0).([]byte), args.Error(1)
}

func (ac *MockAttributionClient) Get(ctx context.Context, resourceType client.ResourceType, id string) ([]byte, error) {
	args := ac.Called(ctx, resourceType, id)
	return args.Get(0).([]byte), args.Error(1)
}

func (ac *MockAttributionClient) Post(ctx context.Context, resourceType client.ResourceType, body []byte) ([]byte, error) {
	args := ac.Called(ctx, resourceType, body)
	return args.Get(0).([]byte), args.Error(1)
}

func (ac *MockAttributionClient) Delete(ctx context.Context, resourceType client.ResourceType, id string) error {
	args := ac.Called(ctx, resourceType, id)
	return args.Error(0)
}

func (ac *MockAttributionClient) Put(ctx context.Context, resourceType client.ResourceType, id string, body []byte) ([]byte, error) {
	args := ac.Called(ctx, resourceType, id, body)
	return args.Get(0).([]byte), args.Error(1)
}

type OrganizationControllerTestSuite struct {
	suite.Suite
	org *OrganizationController
	mac *MockAttributionClient
}

func (suite *OrganizationControllerTestSuite) SetupTest() {
	mac := new(MockAttributionClient)
	suite.mac = mac
	suite.org = NewOrganizationController(mac)

}

func TestOrganizationControllerTestSuite(t *testing.T) {
	suite.Run(t, new(OrganizationControllerTestSuite))
}

func (suite *OrganizationControllerTestSuite) TestReadOrganizationErrorInClient() {
	suite.mac.On("Get", mock.Anything, mock.Anything, mock.Anything).Return(make([]byte, 0), errors.New("Test Error"))

	ja := jsonassert.New(suite.T())

	req := httptest.NewRequest("GET", "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)
	ctx = req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.org.Read(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusNotFound, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
        "issue": [
            {
                "severity": "warning",
                "code": "Not Found",
                "details": {
                    "text": "Failed to find organization"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)
}

func (suite *OrganizationControllerTestSuite) TestReadOrganization() {
	ja := jsonassert.New(suite.T())

	suite.mac.On("Get", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionOrgResponse(), nil)

	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.org.Read(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), string(apitest.AttributionOrgResponse()))

}

func (suite *OrganizationControllerTestSuite) TestCreateOrganizationErrorInClient() {
	suite.mac.On("Post", mock.Anything, mock.Anything, mock.Anything).Return(make([]byte, 0), errors.New("Test Error"))

	ja := jsonassert.New(suite.T())

	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", strings.NewReader(apitest.Orgjson))
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.org.Create(w, req)

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
                    "text": "Failed to save organization"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)

}

func (suite *OrganizationControllerTestSuite) TestCreateOrganizationBadJsonOrg() {
	ja := jsonassert.New(suite.T())

	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.org.Create(w, req)

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
                    "text": "Not a valid organization"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)
}

func (suite *OrganizationControllerTestSuite) TestCreateOrganization() {
	suite.mac.On("Post", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionOrgResponse(), nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", strings.NewReader(apitest.Orgjson))

	w := httptest.NewRecorder()
	suite.org.Create(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)
	ja.Assertf(string(resp), string(apitest.AttributionOrgResponse()))

	req = httptest.NewRequest(http.MethodPost, "http://example.com/foo", bytes.NewReader(apitest.MalformedOrg()))

	w = httptest.NewRecorder()
	suite.org.Create(w, req)
	res = w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)
}

func (suite *OrganizationControllerTestSuite) TestDeleteOrganization() {
	suite.mac.On("Delete", mock.Anything, mock.Anything, mock.Anything).Return(nil)

	req := httptest.NewRequest(http.MethodDelete, "http://example.com/foo/12345", nil)

	w := httptest.NewRecorder()
	suite.org.Delete(w, req)
	res := w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)

	w = httptest.NewRecorder()
	suite.org.Delete(w, req)
	res = w.Result()

	assert.Equal(suite.T(), http.StatusNoContent, res.StatusCode)
}

func (suite *OrganizationControllerTestSuite) TestUpdateOrganizationErrors() {
	req := httptest.NewRequest(http.MethodPut, "http://example.com/foo", strings.NewReader(apitest.Orgjson))

	w := httptest.NewRecorder()
	suite.org.Update(w, req)
	res := w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)

	badReq := httptest.NewRequest(http.MethodPut, "http://example.com/foo", bytes.NewReader(apitest.MalformedOrg()))
	w = httptest.NewRecorder()
	suite.org.Update(w, badReq)
	res = w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	suite.mac.On("Put", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(make([]byte, 0), errors.New("error")).Once()
	w = httptest.NewRecorder()
	suite.org.Update(w, req)
	res = w.Result()

	assert.Equal(suite.T(), http.StatusUnprocessableEntity, res.StatusCode)
}

func (suite *OrganizationControllerTestSuite) TestUpdateOrganization() {
	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodPut, "http://example.com/foo", strings.NewReader(apitest.Orgjson))
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)
	ar := apitest.AttributionOrgResponse()

	suite.mac.On("Put", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(ar, nil).Once()
	w := httptest.NewRecorder()
	suite.org.Update(w, req)
	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)
	ja.Assertf(string(resp), string(ar))
}

func (suite *OrganizationControllerTestSuite) TestExportNotImplemented() {
	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.org.Export(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}
