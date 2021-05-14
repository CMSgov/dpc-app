package middleware

import (
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/CMSgov/dpc/api/apitest"
	"github.com/kinbiko/jsonassert"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type FHIRMiddlewareTestSuite struct {
	suite.Suite
}

func (suite *FHIRMiddlewareTestSuite) SetupTest() {
}

func TestFHIRMiddlewareTestSuite(t *testing.T) {
	suite.Run(t, new(FHIRMiddlewareTestSuite))
}

func (suite *FHIRMiddlewareTestSuite) TestFHIRModel() {
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write(apitest.AttributionOrgResponse())
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.your-domain.com", nil)
	res := httptest.NewRecorder()

	fm := FHIRModel(nextHandler)
	fm.ServeHTTP(res, req)

	b, _ := ioutil.ReadAll(res.Body)

	o, _ := fhir.UnmarshalOrganization(b)
	assert.Equal(suite.T(), *o.Id, "<<PRESENCE>>")
	assert.NotNil(suite.T(), o.Meta)
	assert.NotNil(suite.T(), o.Meta.Id)
	assert.NotNil(suite.T(), o.Meta.LastUpdated)
	assert.NotNil(suite.T(), o.Meta.VersionId)
}

func (suite *FHIRMiddlewareTestSuite) TestFHIRModelError() {
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write([]byte("{}"))
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.your-domain.com", nil)
	res := httptest.NewRecorder()

	fm := FHIRModel(nextHandler)
	fm.ServeHTTP(res, req)

	w := res.Result()
	b, _ := ioutil.ReadAll(w.Body)
	ja := jsonassert.New(suite.T())

	assert.Equal(suite.T(), http.StatusInternalServerError, w.StatusCode)
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

func (suite *FHIRMiddlewareTestSuite) TestFHIRFiltering() {
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		b, _ := ioutil.ReadAll(r.Body)
		o, _ := fhir.UnmarshalOrganization(b)
		assert.Nil(suite.T(), o.Contact)
		assert.Nil(suite.T(), o.Telecom)
		assert.Nil(suite.T(), o.Address)
		assert.NotNil(suite.T(), o.Identifier)
		assert.NotNil(suite.T(), o.Name)
	})

	req := httptest.NewRequest(http.MethodPost, "http://www.your-domain.com", strings.NewReader(apitest.Orgjson))
	res := httptest.NewRecorder()

	fm := FHIRFilter(nextHandler)
	fm.ServeHTTP(res, req)

	req = httptest.NewRequest(http.MethodPut, "http://www.your-domain.com", strings.NewReader(apitest.Orgjson))
	res = httptest.NewRecorder()

	fm = FHIRFilter(nextHandler)
	fm.ServeHTTP(res, req)
}

func (suite *FHIRMiddlewareTestSuite) TestFHIRFilteringOnError() {
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write([]byte("{}"))
	})

	req := httptest.NewRequest(http.MethodPost, "http://www.your-domain.com", nil)
	res := httptest.NewRecorder()

	fm := FHIRFilter(nextHandler)
	fm.ServeHTTP(res, req)

	w := res.Result()
	b, _ := ioutil.ReadAll(w.Body)
	ja := jsonassert.New(suite.T())

	assert.Equal(suite.T(), http.StatusInternalServerError, w.StatusCode)
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

func (suite *FHIRMiddlewareTestSuite) TestFHIRFilteringPassesThruWhenNotPostOrPut() {
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		body, _ := ioutil.ReadAll(r.Body)
		_, _ = w.Write(body)
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.your-domain.com", strings.NewReader(`{"foo":"bar"}`))
	res := httptest.NewRecorder()

	fm := FHIRFilter(nextHandler)
	fm.ServeHTTP(res, req)

	w := res.Result()
	b, _ := ioutil.ReadAll(w.Body)
	ja := jsonassert.New(suite.T())

	ja.Assertf(string(b), `
    {"foo":"bar"}`)
}
