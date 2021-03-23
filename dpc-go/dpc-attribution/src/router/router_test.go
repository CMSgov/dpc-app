package router

import (
	"github.com/CMSgov/dpc/attribution/attributiontest"
	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	"github.com/darahayes/go-boom"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

type MockService struct {
	mock.Mock
}

func (ms *MockService) Get(w http.ResponseWriter, r *http.Request) {
	ms.Called(w, r)
}

func (ms *MockService) Post(w http.ResponseWriter, r *http.Request) {
	ms.Called(w, r)
}

func (ms *MockService) Delete(w http.ResponseWriter, r *http.Request) {
	ms.Called(w, r)
}

func (ms *MockService) Put(w http.ResponseWriter, r *http.Request) {
	ms.Called(w, r)
}

type RouterTestSuite struct {
	suite.Suite
	router    http.Handler
	mockOrg   *MockService
	mockGroup *MockService
}

func TestRouterTestSuite(t *testing.T) {
	suite.Run(t, new(RouterTestSuite))
}

func (suite *RouterTestSuite) SetupTest() {
	suite.mockOrg = &MockService{}
	suite.mockGroup = &MockService{}
	suite.router = NewDPCAttributionRouter(suite.mockOrg, suite.mockGroup)
}

func (suite *RouterTestSuite) do(httpMethod string, route string, body io.Reader) *http.Response {
	req := httptest.NewRequest(httpMethod, route, body)
	req.Header.Set(middleware2.OrgHeader, "1234")
	rr := httptest.NewRecorder()
	suite.router.ServeHTTP(rr, req)
	return rr.Result()
}

func (suite *RouterTestSuite) TestOrganizationGetRoute() {
	suite.mockOrg.On("Get", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		_, _ = w.Write([]byte(attributiontest.Orgjson))
		r := arg.Get(1).(*http.Request)
		assert.Equal(suite.T(), "1234", r.Context().Value(middleware2.ContextKeyOrganization))
	})

	res := suite.do(http.MethodGet, "/Organization/1234", nil)
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	suite.mockOrg.On("Get", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		boom.Internal(w)
	})

	res = suite.do(http.MethodGet, "/Organization/1234", nil)
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusInternalServerError, res.StatusCode)

	res = suite.do(http.MethodGet, "/Organization", strings.NewReader(attributiontest.Orgjson))
	assert.Equal(suite.T(), http.StatusMethodNotAllowed, res.StatusCode)
}

func (suite *RouterTestSuite) TestOrganizationPostRoute() {
	suite.mockOrg.On("Post", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		_, _ = w.Write([]byte(attributiontest.Orgjson))
		r := arg.Get(1).(*http.Request)
		assert.Nil(suite.T(), r.Context().Value(middleware2.ContextKeyOrganization))
	})

	res := suite.do(http.MethodPost, "/Organization", strings.NewReader(attributiontest.Orgjson))
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.NotEqual(suite.T(), res.Body, http.NoBody)

	suite.mockOrg.On("Post", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		boom.Internal(w)
		r := arg.Get(1).(*http.Request)
		assert.Nil(suite.T(), r.Context().Value(middleware2.ContextKeyOrganization))
	})

	res = suite.do(http.MethodPost, "/Organization", strings.NewReader(attributiontest.Orgjson))
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusInternalServerError, res.StatusCode)

	res = suite.do(http.MethodPost, "/Organization/1234", strings.NewReader(attributiontest.Orgjson))
	assert.Equal(suite.T(), http.StatusMethodNotAllowed, res.StatusCode)
}

func (suite *RouterTestSuite) TestOrganizationDeleteRoute() {
	suite.mockOrg.On("Delete", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		w.WriteHeader(http.StatusNoContent)
		r := arg.Get(1).(*http.Request)
		assert.Equal(suite.T(), "1234", r.Context().Value(middleware2.ContextKeyOrganization))
	})

	res := suite.do(http.MethodDelete, "/Organization/1234", nil)
	assert.Equal(suite.T(), http.StatusNoContent, res.StatusCode)

	res = suite.do(http.MethodDelete, "/Organization", strings.NewReader(attributiontest.Orgjson))
	assert.Equal(suite.T(), http.StatusMethodNotAllowed, res.StatusCode)
}

func (suite *RouterTestSuite) TestOrganizationPutRoute() {
	suite.mockOrg.On("Put", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		_, _ = w.Write([]byte(attributiontest.Orgjson))
		r := arg.Get(1).(*http.Request)
		assert.Equal(suite.T(), "1234", r.Context().Value(middleware2.ContextKeyOrganization))
	})

	res := suite.do(http.MethodPut, "/Organization/1234", strings.NewReader(attributiontest.Orgjson))
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.NotEqual(suite.T(), res.Body, http.NoBody)

	res = suite.do(http.MethodPut, "/Organization", strings.NewReader(attributiontest.Orgjson))
	assert.Equal(suite.T(), http.StatusMethodNotAllowed, res.StatusCode)
}
