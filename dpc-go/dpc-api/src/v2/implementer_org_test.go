package v2

import (
	"context"
	"github.com/CMSgov/dpc/api/apitest"
	"github.com/go-chi/chi/middleware"
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

type ImplementerOrgControllerTestSuite struct {
	suite.Suite
	implOrg *ImplementerOrgController
	mac     *MockAttributionClient
}

func (suite *ImplementerOrgControllerTestSuite) SetupTest() {
	mac := new(MockAttributionClient)
	suite.mac = mac
	suite.implOrg = NewImplementerOrgController(mac)
}

func TestImplementerOrgControllerTestSuite(t *testing.T) {
	suite.Run(t, new(ImplementerOrgControllerTestSuite))
}

func (suite *ImplementerOrgControllerTestSuite) TestCreateImplementerOrg() {
	suite.mac.On("CreateImplOrg", mock.Anything, mock.Anything).Return(apitest.AttributionToFHIRResponse(apitest.ImplOrgJSON), nil)
	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", strings.NewReader(apitest.ImplOrgJSON))
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	suite.implOrg.Create(w, req)
	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
}

func (suite *ImplementerOrgControllerTestSuite) TestCreateImplementerOrgMissingBody() {
	ja := jsonassert.New(suite.T())

	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	suite.implOrg.Create(w, req)
	res := w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)
	ja.Assertf(string(resp), `
      {
          "issue": [
              {
                  "severity": "warning",
                  "code": "Business Rule Violation",
                  "details": {
                      "text": "Body is required"
                  },
                  "diagnostics": "12345"
              }
          ],
          "resourceType": "OperationOutcome"
      }
  `)
}
