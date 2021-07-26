package public

import (
    "encoding/json"
    "fmt"
    "io/ioutil"
    "net/http"
    "net/http/httptest"
    "testing"

    "github.com/go-chi/chi/middleware"
    "github.com/stretchr/testify/assert"
    "github.com/stretchr/testify/mock"
    "github.com/stretchr/testify/suite"

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

type RouterTestSuite struct {
	suite.Suite
	router      http.Handler
	mockMeta    *MockController
	mockGroup   *MockController
	mockData    *MockFileController
	mockJob     *MockJobController
}

func (suite *RouterTestSuite) SetupTest() {
	suite.mockMeta = &MockController{}
	suite.mockGroup = &MockController{}
	suite.mockData = &MockFileController{}
	suite.mockJob = &MockJobController{}

	suite.router = buildPublicRoutes(suite.mockMeta, suite.mockGroup, suite.mockData, suite.mockJob)
}

func TestRouterTestSuite(t *testing.T) {
	suite.Run(t, new(RouterTestSuite))
}

func (suite *RouterTestSuite) TestMetadataRoute() {
	suite.mockMeta.On("Read", mock.Anything, mock.Anything).Once()

	ts := httptest.NewServer(suite.router)

	res, _ := http.Get(fmt.Sprintf("%s/%s", ts.URL, "v2/metadata"))

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
}

func (suite *RouterTestSuite) TestGroupExportRoute() {
	var capturedRequestID string
	suite.mockGroup.On("Export", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		capturedRequestID = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		w.WriteHeader(http.StatusAccepted)
	})

	ts := httptest.NewServer(suite.router)

	req, _ := http.NewRequest(http.MethodGet, fmt.Sprintf("%s/%s", ts.URL, "v2/Group/9876/$export"), nil)
	req.Header.Set("Content-Type", "application/fhir+json")
	req.Header.Set("Prefer", "respond-async")
	req.Header.Set(middleware.RequestIDHeader, "54321")
	req.Header.Set(middleware2.OrgHeader, "12345")
	res, _ := http.DefaultClient.Do(req)

	b, _ := ioutil.ReadAll(res.Body)
	var v map[string]interface{}
	_ = json.Unmarshal(b, &v)

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.NotNil(suite.T(), res.Header.Get("Content-Location"))
	assert.Equal(suite.T(), http.StatusAccepted, res.StatusCode)
	assert.Equal(suite.T(), "54321", capturedRequestID)
	assert.Nil(suite.T(), v)
}
