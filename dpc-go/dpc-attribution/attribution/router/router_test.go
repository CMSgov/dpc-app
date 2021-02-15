package router

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/attributiontest"
	"github.com/CMSgov/dpc/attribution/model"
	v2 "github.com/CMSgov/dpc/attribution/v2"
	"github.com/darahayes/go-boom"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"
)

type MockService struct {
	mock.Mock
}

func (ms *MockService) Get(w http.ResponseWriter, r *http.Request) {
	ms.Called(w, r)
}

func (ms *MockService) Save(w http.ResponseWriter, r *http.Request) {
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

func (suite *RouterTestSuite) TestOrganizationGetRoutes() {
	mockOrg := new(MockService)

	mockOrg.On("Get", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		b, _ := json.Marshal(suite.fakeOrg)
		w.Write(b)
	})

	router := suite.r(mockOrg)
	ts := httptest.NewServer(router)

	res, _ := http.Get(fmt.Sprintf("%s/%s", ts.URL, "Organization/1234"))

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	mockOrg.On("Get", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		boom.Internal(w)
	})

	res, _ = http.Get(fmt.Sprintf("%s/%s", ts.URL, "Organization/1234"))

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusInternalServerError, res.StatusCode)

}

func (suite *RouterTestSuite) TestOrganizationPostRoutes() {
	mockOrg := new(MockService)

	mockOrg.On("Save", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		b, _ := json.Marshal(suite.fakeOrg)
		w.Write(b)
	})

	router := suite.r(mockOrg)
	ts := httptest.NewServer(router)

	b, _ := json.Marshal(suite.fakeOrg)

	res, _ := http.Post(fmt.Sprintf("%s/%s", ts.URL, "Organization"), "application/json", bytes.NewReader(b))
	body, _ := ioutil.ReadAll(res.Body)
	var actual *model.Organization
	_ = json.Unmarshal(body, &actual)

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	assert.Equal(suite.T(), suite.fakeOrg.ID, actual.ID)

	mockOrg.On("Save", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		boom.Internal(w)
	})

	res, _ = http.Post(fmt.Sprintf("%s/%s", ts.URL, "Organization"), "application/json", bytes.NewReader(b))

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusInternalServerError, res.StatusCode)

}
