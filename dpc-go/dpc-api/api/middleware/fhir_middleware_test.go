package middleware

import (
	"github.com/CMSgov/dpc/api/apitest"
	"github.com/kinbiko/jsonassert"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"
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
		w.Write(apitest.AttributionResponse())
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.your-domain.com", nil)
	res := httptest.NewRecorder()

	fm := FHIRModel(nextHandler)
	fm.ServeHTTP(res, req)

	b, _ := ioutil.ReadAll(res.Body)
	ja := jsonassert.New(suite.T())

	ja.Assertf(string(b), `{
      "address": [
        {
          "city": "PLYMOUTH",
          "country": "US",
          "line": [
            "275 SANDWICH STREET"
          ],
          "postalCode": "02360",
          "state": "MA",
          "type": "both",
          "use": "work"
        }
      ],
      "id": "<<PRESENCE>>",
      "identifier": [
        {
          "system": "http://hl7.org/fhir/sid/us-npi",
          "value": "2111111119"
        }
      ],
      "meta": "<<PRESENCE>>",
      "name": "BETH ISRAEL DEACONESS HOSPITAL - PLYMOUTH",
      "resourceType": "Organization",
      "telecom": [
        {
          "system": "phone",
          "value": "5087462000"
        }
      ],
      "type": [
        {
          "coding": [
            {
              "code": "prov",
              "display": "Healthcare Provider",
              "system": "http://terminology.hl7.org/CodeSystem/organization-type"
            }
          ],
          "text": "Healthcare Provider"
        }
      ]
    }`)
}

func (suite *FHIRMiddlewareTestSuite) TestFHIRModelError() {
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Write([]byte("{}"))
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
