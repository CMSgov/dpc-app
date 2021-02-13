package api

import (
	"encoding/json"
	"fmt"
	v2 "github.com/CMSgov/dpc/api/v2"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

const orgjson = `{
  "ID": "12345",
  "VersionId": "0",
  "LastUpdated": "2017-01-01T00:00:00.000Z",
  "Info": {
    "resourceType": "Organization",
    "identifier": [
      {
        "system": "http://hl7.org/fhir/sid/us-npi",
        "value": "2111111119"
      }
    ],
    "type": [
      {
        "coding": [
          {
            "system": "http://terminology.hl7.org/CodeSystem/organization-type",
            "code": "prov",
            "display": "Healthcare Provider"
          }
        ],
        "text": "Healthcare Provider"
      }
    ],
    "name": "BETH ISRAEL DEACONESS HOSPITAL - PLYMOUTH",
    "telecom": [
      {
        "system": "phone",
        "value": "5087462000"
      }
    ],
    "address": [
      {
        "use": "work",
        "type": "both",
        "line": [
          "275 SANDWICH STREET"
        ],
        "city": "PLYMOUTH",
        "state": "MA",
        "postalCode": "02360",
        "country": "US"
      }
    ]
  }
}`

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
		w.Write([]byte(orgjson))
	})

	router := suite.r(mockOrg, mockMeta)
	ts := httptest.NewServer(router)

	res, _ := http.Get(fmt.Sprintf("%s/%s", ts.URL, "v2/Organization/1234"))

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	b, _ := ioutil.ReadAll(res.Body)
	var v map[string]interface{}
	_ = json.Unmarshal(b, &v)
	assert.Nil(suite.T(), v["Info"])

	mockOrg.On("Read", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		w.Write([]byte("{}"))
	})

	res, _ = http.Get(fmt.Sprintf("%s/%s", ts.URL, "v2/Organization/1234"))

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusInternalServerError, res.StatusCode)

}

func (suite *RouterTestSuite) TestOrganizationPostRoutes() {
	mockMeta := new(MockController)
	mockOrg := new(MockController)

	mockOrg.On("Create", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		w.Write([]byte(orgjson))
	})

	router := suite.r(mockOrg, mockMeta)
	ts := httptest.NewServer(router)

	res, _ := http.Post(fmt.Sprintf("%s/%s", ts.URL, "v2/Organization"), "application/fhir+json", strings.NewReader(orgjson))

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	b, _ := ioutil.ReadAll(res.Body)
	var v map[string]interface{}
	_ = json.Unmarshal(b, &v)
	assert.Nil(suite.T(), v["Info"])

}

func (suite *RouterTestSuite) TestOrganizationDeleteRoutes() {
	mockMeta := new(MockController)
	mockOrg := new(MockController)

	mockOrg.On("Delete", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		w.WriteHeader(http.StatusNoContent)
	})

	router := suite.r(mockOrg, mockMeta)
	ts := httptest.NewServer(router)

	req, _ := http.NewRequest("DELETE", fmt.Sprintf("%s/%s", ts.URL, "v2/Organization/12345"), nil)
	res, _ := http.DefaultClient.Do(req)

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusNoContent, res.StatusCode)
}

func (suite *RouterTestSuite) TestOrganizationPutRoutes() {
	mockMeta := new(MockController)
	mockOrg := new(MockController)

	var orgID string
	mockOrg.On("Update", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		r := arg.Get(1).(*http.Request)
		orgID = r.Context().Value(v2.ContextKeyOrganization).(string)
		w := arg.Get(0).(http.ResponseWriter)
		w.Write([]byte(orgjson))
	})

	router := suite.r(mockOrg, mockMeta)
	ts := httptest.NewServer(router)

	req, _ := http.NewRequest("PUT", fmt.Sprintf("%s/%s", ts.URL, "v2/Organization/12345"), strings.NewReader(orgjson))
	res, _ := http.DefaultClient.Do(req)

	assert.Equal(suite.T(), "application/fhir+json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.Equal(suite.T(), "12345", orgID)

	b, _ := ioutil.ReadAll(res.Body)
	var v map[string]interface{}
	_ = json.Unmarshal(b, &v)
	assert.Nil(suite.T(), v["Info"])
}
