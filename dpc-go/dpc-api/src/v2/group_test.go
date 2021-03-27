package v2

import (
	"bytes"
	"context"
	"github.com/CMSgov/dpc/api/apitest"
	"github.com/go-chi/chi/middleware"
	"github.com/kinbiko/jsonassert"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

type GroupControllerTestSuite struct {
	suite.Suite
	grp *GroupController
	mac *MockAttributionClient
}

func (suite *GroupControllerTestSuite) SetupTest() {
	mac := new(MockAttributionClient)
	suite.mac = mac
	suite.grp = NewGroupController(mac)

}

func TestGroupControllerTestSuite(t *testing.T) {
	suite.Run(t, new(GroupControllerTestSuite))
}

func (suite *GroupControllerTestSuite) TestCreateGroupErrorInClient() {
	suite.mac.On("Post", mock.Anything, mock.Anything, mock.Anything).Return(make([]byte, 0), errors.New("Test Error"))

	ja := jsonassert.New(suite.T())

	req := httptest.NewRequest(http.MethodPost, "http://example.com/Group", strings.NewReader(apitest.Groupjson))
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.grp.Create(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusUnprocessableEntity, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
        "issue": [
            {
                "severity": "error",
                "code": "Exception",
                "details": {
                    "text": "Failed to save group"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)

}

func (suite *GroupControllerTestSuite) TestCreateGroupBadJson() {
	ja := jsonassert.New(suite.T())

	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.grp.Create(w, req)

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
                    "text": "Not a valid group"
                },
                "diagnostics": "12345"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)
}

func (suite *GroupControllerTestSuite) TestCreateGroup() {
	suite.mac.On("Post", mock.Anything, mock.Anything, mock.Anything).Return(apitest.AttributionResponse(apitest.Groupjson), nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", strings.NewReader(apitest.Groupjson))

	w := httptest.NewRecorder()
	suite.grp.Create(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)
	ja.Assertf(string(resp), string(apitest.AttributionResponse(apitest.Groupjson)))

	req = httptest.NewRequest(http.MethodPost, "http://example.com/foo", bytes.NewReader(apitest.MalformedOrg()))

	w = httptest.NewRecorder()
	suite.grp.Create(w, req)
	res = w.Result()

	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)
}
