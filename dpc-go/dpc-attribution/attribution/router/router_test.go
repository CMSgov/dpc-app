package router

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/attributiontest"
	"github.com/CMSgov/dpc/attribution/model"
	v2 "github.com/CMSgov/dpc/attribution/v2"
	"github.com/darahayes/go-boom"
	"github.com/go-chi/chi/middleware"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
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
	r       func(os v2.Service) http.Handler
	fakeOrg *model.Organization
}

func (suite *RouterTestSuite) SetupTest() {
	suite.r = NewDPCAttributionRouter
	suite.fakeOrg = attributiontest.OrgResponse()
}

func TestRouterTestSuite(t *testing.T) {
	suite.Run(t, new(RouterTestSuite))
}

func (suite *RouterTestSuite) TestOrganizationGetRoute() {
	mockOrg := new(MockService)

	var capturedRequestId string
	mockOrg.On("Get", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		capturedRequestId = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		b, _ := json.Marshal(suite.fakeOrg)
		w.Write(b)
	})

	router := suite.r(mockOrg)
	ts := httptest.NewServer(router)

	req, _ := http.NewRequest(http.MethodGet, fmt.Sprintf("%s/%s", ts.URL, "Organization/1234"), nil)
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ := http.DefaultClient.Do(req)

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.Equal(suite.T(), "54321", capturedRequestId)

	mockOrg.On("Get", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		capturedRequestId = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		boom.Internal(w)
	})

	req, _ = http.NewRequest(http.MethodGet, fmt.Sprintf("%s/%s", ts.URL, "Organization/1234"), nil)
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ = http.DefaultClient.Do(req)

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusInternalServerError, res.StatusCode)
	assert.Equal(suite.T(), "54321", capturedRequestId)
}

func (suite *RouterTestSuite) TestOrganizationPostRoute() {
	mockOrg := new(MockService)

	var capturedRequestId string
	mockOrg.On("Post", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		capturedRequestId = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		b, _ := json.Marshal(suite.fakeOrg)
		w.Write(b)
	})

	router := suite.r(mockOrg)
	ts := httptest.NewServer(router)

	b, _ := json.Marshal(suite.fakeOrg)

	req, _ := http.NewRequest(http.MethodPost, fmt.Sprintf("%s/%s", ts.URL, "Organization"), bytes.NewReader(b))
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ := http.DefaultClient.Do(req)
	body, _ := ioutil.ReadAll(res.Body)
	var actual *model.Organization
	_ = json.Unmarshal(body, &actual)

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.Equal(suite.T(), suite.fakeOrg.ID, actual.ID)
	assert.Equal(suite.T(), "54321", capturedRequestId)

	mockOrg.On("Post", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		capturedRequestId = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		boom.Internal(w)
	})

	req, _ = http.NewRequest(http.MethodPost, fmt.Sprintf("%s/%s", ts.URL, "Organization"), bytes.NewReader(b))
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ = http.DefaultClient.Do(req)

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusInternalServerError, res.StatusCode)
	assert.Equal(suite.T(), "54321", capturedRequestId)
}

func (suite *RouterTestSuite) TestOrganizationDeleteRoute() {
	mockOrg := new(MockService)

	var capturedRequestId string
	mockOrg.On("Delete", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		capturedRequestId = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		w.WriteHeader(http.StatusNoContent)
	})

	router := suite.r(mockOrg)
	ts := httptest.NewServer(router)

	req, _ := http.NewRequest(http.MethodDelete, fmt.Sprintf("%s/%s", ts.URL, "Organization/12345"), nil)
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ := http.DefaultClient.Do(req)

	assert.Equal(suite.T(), http.StatusNoContent, res.StatusCode)
	assert.Equal(suite.T(), "54321", capturedRequestId)

}

func (suite *RouterTestSuite) TestOrganizationPutRoute() {
	mockOrg := new(MockService)

	var capturedRequestId string
	mockOrg.On("Put", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		capturedRequestId = r.Header.Get(middleware.RequestIDHeader)
		w := arg.Get(0).(http.ResponseWriter)
		b, _ := json.Marshal(suite.fakeOrg)
		w.Write(b)
	})

	router := suite.r(mockOrg)
	ts := httptest.NewServer(router)

	req, _ := http.NewRequest(http.MethodPut, fmt.Sprintf("%s/%s", ts.URL, "Organization/12345"), strings.NewReader(attributiontest.Orgjson))
	req.Header.Set(middleware.RequestIDHeader, "54321")
	res, _ := http.DefaultClient.Do(req)

	body, _ := ioutil.ReadAll(res.Body)
	var actual *model.Organization
	_ = json.Unmarshal(body, &actual)

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.Equal(suite.T(), suite.fakeOrg.ID, actual.ID)
	assert.Equal(suite.T(), "54321", capturedRequestId)
}
