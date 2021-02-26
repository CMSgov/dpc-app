package v2

import (
	"context"
	"github.com/CMSgov/dpc/api/apitest"
	"github.com/CMSgov/dpc/api/client"
	"github.com/go-chi/chi/middleware"
	"github.com/kinbiko/jsonassert"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

type MockAttributionClient struct {
	mock.Mock
}

func (ac *MockAttributionClient) Get(ctx context.Context, resourceType client.ResourceType, id string) ([]byte, error) {
	args := ac.Called(ctx, resourceType, id)
	return args.Get(0).([]byte), args.Error(1)
}

func (ac *MockAttributionClient) Post(ctx context.Context, resourceType client.ResourceType, organization []byte) ([]byte, error) {
	args := ac.Called(ctx, resourceType, organization)
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

func (suite *OrganizationControllerTestSuite) TestGetOrganizationErrorInClient() {
	suite.mac.On("Get", mock.Anything, mock.Anything, mock.Anything).Return(make([]byte, 0), errors.New("Test Error"))

	ja := jsonassert.New(suite.T())

	req := httptest.NewRequest("GET", "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, ContextKeyOrganization, "12345")
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

func (suite *OrganizationControllerTestSuite) TestGetOrganization() {
	ja := jsonassert.New(suite.T())

	suite.mac.On("Get", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionResponse(), nil)

	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.org.Read(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), string(apitest.AttributionResponse()))

}

func (suite *OrganizationControllerTestSuite) TestPostOrganizationErrorInClient() {
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

func (suite *OrganizationControllerTestSuite) TestPostOrganizationBadJsonOrg() {
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

func (suite *OrganizationControllerTestSuite) TestPostOrganization() {
	suite.mac.On("Post", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionResponse(), nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", strings.NewReader(apitest.Orgjson))

	w := httptest.NewRecorder()

	suite.org.Create(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), string(apitest.AttributionResponse()))
}
