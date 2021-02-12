package v2

import (
	"github.com/kinbiko/jsonassert"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"
)

type MetadataControllerTestSuite struct {
	suite.Suite
	meta *MetadataController
}

func (suite *MetadataControllerTestSuite) SetupTest() {
	suite.meta = NewMetadataController("../../DPCCapabilities.json")
}

func TestMetadataControllerTestSuite(t *testing.T) {
	suite.Run(t, new(MetadataControllerTestSuite))
}

func (suite *MetadataControllerTestSuite) TestMetadata() {

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest("GET", "http://example.com/foo", nil)

	w := httptest.NewRecorder()

	suite.meta.Read(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
      "resourceType": "CapabilityStatement",
      "description": "This Capability Statement defines the available resource, endpoints and operations supported by the Data @ the Point of Care Application.",
      "id": "dpc-capabilities",
      "status": "draft",
      "date": "<<PRESENCE>>",
      "publisher": "Centers for Medicare and Medicaid Services",
      "kind": "instance",
      "software": {
        "name": "Data @ Point of Care API",
        "version": "<<PRESENCE>>",
        "releaseDate": "<<PRESENCE>>"
      },
      "fhirVersion": "4.0.1",
      "format": [
        "application/json",
        "application/fhir+json"
      ],
      "rest": [
        {
          "mode": "server",
          "security": {
            "cors": true
          },
          "resource": [
            {
              "type": "Organization",
              "interaction": [
                {
                  "code": "read"
                }
              ]
            }
          ]
        }
      ]
    }`)

}
