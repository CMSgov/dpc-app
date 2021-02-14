package router

import (
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/apitest"
	"github.com/CMSgov/dpc/api/fhirror"
	v2 "github.com/CMSgov/dpc/api/v2"
	"github.com/kinbiko/jsonassert"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

type MockController struct {
	mock.Mock
}

func (c *MockController) Read(w http.ResponseWriter, r *http.Request) {
	c.Called(w, r)
}

func (c *MockController) Create(w http.ResponseWriter, r *http.Request) {
	c.Called(w, r)
}

type RouterTestSuite struct {
	suite.Suite
	r func(oc v2.Controller, mc v2.ReadController) http.Handler
}

func (suite *RouterTestSuite) SetupTest() {
	suite.r = NewDPCAPIRouter
}

func TestRouterTestSuite(t *testing.T) {
	suite.Run(t, new(RouterTestSuite))
}

func (suite *RouterTestSuite) TestErrorHandling() {
	mockMeta := new(MockController)
	mockOrg := new(MockController)

	mockOrg.On("Read", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		r := arg.Get(1).(*http.Request)
		fhirror.GenericServerIssue(w, r.Context())
	})

	router := suite.r(mockOrg, mockMeta)
	ts := httptest.NewServer(router)
	res, _ := http.Get(fmt.Sprintf("%s/%s", ts.URL, "v2/Organization/12345"))

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
          "diagnostics": "<<PRESENCE>>"
        }
      ],
      "resourceType": "OperationOutcome"
    }`)
}

func (suite *RouterTestSuite) TestMetadataRoute() {
	mockMeta := new(MockController)
	mockOrg := new(MockController)

	mockMeta.On("Read", mock.Anything, mock.Anything).Once()

	router := suite.r(mockOrg, mockMeta)
	ts := httptest.NewServer(router)

	res, _ := http.Get(fmt.Sprintf("%s/%s", ts.URL, "v2/metadata"))

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
}

func (suite *RouterTestSuite) TestOrganizationGetRoutes() {
	mockMeta := new(MockController)
	mockOrg := new(MockController)

	mockOrg.On("Read", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		w.Write(apitest.AttributionResponse())
	})

	router := suite.r(mockOrg, mockMeta)
	ts := httptest.NewServer(router)

	res, _ := http.Get(fmt.Sprintf("%s/%s", ts.URL, "v2/Organization/1234"))

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	b, _ := ioutil.ReadAll(res.Body)
	var v map[string]interface{}
	_ = json.Unmarshal(b, &v)
	assert.Nil(suite.T(), v["info"])
	assert.NotNil(suite.T(), v["resourceType"])
	assert.Equal(suite.T(), v["resourceType"], "Organization")
}

func (suite *RouterTestSuite) TestOrganizationPostRoutes() {
	mockMeta := new(MockController)
	mockOrg := new(MockController)

	mockOrg.On("Create", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		w.Write(apitest.AttributionResponse())
	})

	router := suite.r(mockOrg, mockMeta)
	ts := httptest.NewServer(router)

	res, _ := http.Post(fmt.Sprintf("%s/%s", ts.URL, "v2/Organization"), "application/fhir+json", strings.NewReader(apitest.Orgjson))

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	b, _ := ioutil.ReadAll(res.Body)
	var v map[string]interface{}
	_ = json.Unmarshal(b, &v)
	assert.Nil(suite.T(), v["info"])
	assert.NotNil(suite.T(), v["resourceType"])
	assert.Equal(suite.T(), v["resourceType"], "Organization")
}
