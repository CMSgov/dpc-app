package attribution

import (
	"bytes"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/model"
	v2 "github.com/CMSgov/dpc/attribution/v2"
	"github.com/bxcodec/faker"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
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
	o := model.Organization{}
	_ = faker.FakeData(&o)
	var i model.Info
	_ = json.Unmarshal([]byte(orgjson), &i)
	o.Info = i
	suite.fakeOrg = &o
}

func TestRouterTestSuite(t *testing.T) {
	suite.Run(t, new(RouterTestSuite))
}

func (suite *RouterTestSuite) TestOrganizationGetRoute() {
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
		w.Write([]byte("{}"))
	})

	res, _ = http.Get(fmt.Sprintf("%s/%s", ts.URL, "Organization/1234"))

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

}

func (suite *RouterTestSuite) TestOrganizationPostRoute() {
	mockOrg := new(MockService)

	mockOrg.On("Post", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		b, _ := json.Marshal(suite.fakeOrg)
		w.Write(b)
	})

	router := suite.r(mockOrg)
	ts := httptest.NewServer(router)

	var m = make(map[string]interface{})
	_ = json.Unmarshal([]byte(orgjson), &m)

	b, _ := json.Marshal(m["Info"])
	r := bytes.NewReader(b)
	res, _ := http.Post(fmt.Sprintf("%s/%s", ts.URL, "Organization"), "application/json", r)

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	body, _ := ioutil.ReadAll(res.Body)
	var actual *model.Organization
	_ = json.Unmarshal(body, &actual)

	assert.Equal(suite.T(), suite.fakeOrg.ID, actual.ID)

}

func (suite *RouterTestSuite) TestOrganizationDeleteRoute() {
	mockOrg := new(MockService)

	mockOrg.On("Delete", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		w.WriteHeader(http.StatusNoContent)
	})

	router := suite.r(mockOrg)
	ts := httptest.NewServer(router)

	req, _ := http.NewRequest("DELETE", fmt.Sprintf("%s/%s", ts.URL, "Organization/12345"), nil)
	res, _ := http.DefaultClient.Do(req)

	assert.Equal(suite.T(), http.StatusNoContent, res.StatusCode)

}

func (suite *RouterTestSuite) TestOrganizationPutRoute() {
	mockOrg := new(MockService)

	mockOrg.On("Put", mock.Anything, mock.Anything).Once().Run(func(arg mock.Arguments) {
		w := arg.Get(0).(http.ResponseWriter)
		b, _ := json.Marshal(suite.fakeOrg)
		w.Write(b)
	})

	router := suite.r(mockOrg)
	ts := httptest.NewServer(router)

	req, _ := http.NewRequest("PUT", fmt.Sprintf("%s/%s", ts.URL, "Organization/12345"), nil)
	res, _ := http.DefaultClient.Do(req)

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	body, _ := ioutil.ReadAll(res.Body)
	var actual *model.Organization
	_ = json.Unmarshal(body, &actual)

	assert.Equal(suite.T(), suite.fakeOrg.ID, actual.ID)

}
