package admin

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/go-chi/chi/middleware"
	"github.com/kinbiko/jsonassert"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"

	"github.com/CMSgov/dpc/api/apitest"
	"github.com/CMSgov/dpc/api/fhirror"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
)

type MockController struct {
	mock.Mock
}

func (c *MockController) Update(w http.ResponseWriter, r *http.Request) {
	c.Called(w, r)
}

func (c *MockController) Read(w http.ResponseWriter, r *http.Request) {
	c.Called(w, r)
}

func (c *MockController) Create(w http.ResponseWriter, r *http.Request) {
	c.Called(w, r)
}

func (c *MockController) Delete(w http.ResponseWriter, r *http.Request) {
	c.Called(w, r)
}

func (c *MockController) Export(w http.ResponseWriter, r *http.Request) {
	c.Called(w, r)
}

type MockFileController struct {
	mock.Mock
}

func (mfc *MockFileController) GetFile(w http.ResponseWriter, r *http.Request) {
	mfc.Called(w, r)
}

type MockJobController struct {
	mock.Mock
}

func (mjc *MockJobController) Status(w http.ResponseWriter, r *http.Request) {
	mjc.Called(w, r)
}

type MockSsasController struct {
	mock.Mock
}

func (mjc *MockSsasController) CreateSystem(w http.ResponseWriter, r *http.Request) {
	mjc.Called(w, r)
}

func (mjc *MockSsasController) GetSystem(w http.ResponseWriter, r *http.Request) {
	mjc.Called(w, r)
}

func (mjc *MockSsasController) GetAuthToken(w http.ResponseWriter, r *http.Request) {
	mjc.Called(w, r)
}

type RouterTestSuite struct {
	suite.Suite
	router      http.Handler
	mockOrg     *MockController
	mockImpl    *MockController
	mockImplOrg *MockController
	mockSsas    *MockSsasController
}

func (suite *RouterTestSuite) SetupTest() {

	suite.mockOrg = &MockController{}
	suite.mockImpl = &MockController{}
	suite.mockImplOrg = &MockController{}
	suite.mockSsas = &MockSsasController{}

	c := controllers{
		Org:     suite.mockOrg,
		Impl:    suite.mockImpl,
		ImplOrg: suite.mockImplOrg,
		Ssas:    suite.mockSsas,
	}

	suite.router = buildAdminRoutes(c)
}

func TestRouterTestSuite(t *testing.T) {
	suite.Run(t, new(RouterTestSuite))
}

func (suite *RouterTestSuite) TestErrorHandling() {

	suite.mockOrg.On("Delete", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		r := arg.Get(1).(*http.Request)
		fhirror.GenericServerIssue(r.Context(), w)
	})

	ts := httptest.NewServer(suite.router)

	req, _ := http.NewRequest(http.MethodDelete, fmt.Sprintf("%s/%s", ts.URL, "v2/Organization/12345"), nil)
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ := http.DefaultClient.Do(req)

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusInternalServerError, res.StatusCode)

	b, _ := ioutil.ReadAll(res.Body)
	fmt.Println(string(b))
	ja := jsonassert.New(suite.T())
	ja.Assertf(string(b), `
    {
      "issue": [
        {
          "severity": "error",
          "code": "Exception",
          "details": {
            "text": "Internal Server Error"
          },
          "diagnostics": "54321"
        }
      ],
      "resourceType": "OperationOutcome"
    }`)
}

func (suite *RouterTestSuite) TestOrganizationPostRoute() {
	var capturedRequestID string
	suite.mockOrg.On("Create", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		capturedRequestID = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		_, _ = w.Write(apitest.AttributionOrgResponse())
	})

	ts := httptest.NewServer(suite.router)

	req, _ := http.NewRequest(http.MethodPost, fmt.Sprintf("%s/%s", ts.URL, "v2/Organization"), strings.NewReader(apitest.Orgjson))
	req.Header.Set("Content-Type", "application/fhir+json")
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ := http.DefaultClient.Do(req)

	b, _ := ioutil.ReadAll(res.Body)
	var v map[string]interface{}
	_ = json.Unmarshal(b, &v)

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.Equal(suite.T(), "54321", capturedRequestID)
	assert.NotNil(suite.T(), v)
	assert.NotContains(suite.T(), v, "info")
	assert.Contains(suite.T(), v, "resourceType")
	assert.Equal(suite.T(), v["resourceType"], "Organization")
}

func (suite *RouterTestSuite) TestOrganizationDeleteRoutes() {
	var capturedRequestID string
	suite.mockOrg.On("Delete", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		capturedRequestID = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		w.WriteHeader(http.StatusNoContent)
	})

	ts := httptest.NewServer(suite.router)

	req, _ := http.NewRequest("DELETE", fmt.Sprintf("%s/%s", ts.URL, "v2/Organization/12345"), nil)
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ := http.DefaultClient.Do(req)

	b, _ := ioutil.ReadAll(res.Body)

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusNoContent, res.StatusCode)
	assert.Equal(suite.T(), "54321", capturedRequestID)
	assert.Empty(suite.T(), b)
}

func (suite *RouterTestSuite) TestOrganizationPutRoutes() {
	var orgID string
	var capturedRequestID string
	suite.mockOrg.On("Update", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		orgID = r.Context().Value(middleware2.ContextKeyOrganization).(string)
		capturedRequestID = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		_, _ = w.Write(apitest.AttributionOrgResponse())
	})

	ts := httptest.NewServer(suite.router)

	req, _ := http.NewRequest(http.MethodPut, fmt.Sprintf("%s/%s", ts.URL, "v2/Organization/12345"), strings.NewReader(apitest.Orgjson))
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ := http.DefaultClient.Do(req)

	b, _ := ioutil.ReadAll(res.Body)
	var v map[string]interface{}
	_ = json.Unmarshal(b, &v)

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.Equal(suite.T(), "12345", orgID)
	assert.Equal(suite.T(), "54321", capturedRequestID)
	assert.NotContains(suite.T(), v, "info")
	assert.Contains(suite.T(), v, "resourceType")
	assert.Equal(suite.T(), v["resourceType"], "Organization")
}

func (suite *RouterTestSuite) TestPostSystemProxyRoute() {
	suite.mockSsas.On("CreateSystem", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		w.WriteHeader(http.StatusOK)
	})

	ts := httptest.NewServer(suite.router)

	req, _ := http.NewRequest("POST", fmt.Sprintf("%s/%s", ts.URL, "v2/Implementer/12345/Org/123/system"), strings.NewReader("{}"))
	req.Header.Set("Content-Type", "application/json")
	res, err := http.DefaultClient.Do(req)
	fmt.Println(err)
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
}

func (suite *RouterTestSuite) TestOrganizationGetRoutes() {
	var capturedRequestID string
	suite.mockOrg.On("Read", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		capturedRequestID = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		_, _ = w.Write(apitest.AttributionOrgResponse())
	})

	ts := httptest.NewServer(suite.router)

	req, _ := http.NewRequest(http.MethodGet, fmt.Sprintf("%s/%s", ts.URL, "v2/Organization/12345"), nil)
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ := http.DefaultClient.Do(req)

	b, _ := ioutil.ReadAll(res.Body)
	var v map[string]interface{}
	_ = json.Unmarshal(b, &v)

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.Equal(suite.T(), "54321", capturedRequestID)
	assert.NotNil(suite.T(), v)
	assert.NotContains(suite.T(), v, "info")
	assert.Contains(suite.T(), v, "resourceType")
	assert.Equal(suite.T(), v["resourceType"], "Organization")
}
