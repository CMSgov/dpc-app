package v2

import (
	"context"
	"encoding/json"
	"github.com/CMSgov/dpc/api/apitest"
	"github.com/CMSgov/dpc/api/client"
	"github.com/bxcodec/faker/v3"
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

type ImplementerControllerTestSuite struct {
	suite.Suite
	impl *ImplementerController
	mac  *MockAttributionClient
	msc  *MockSsasClient
}

func (suite *ImplementerControllerTestSuite) SetupTest() {
	mac := new(MockAttributionClient)
	msc := new(MockSsasClient)
	suite.mac = mac
	suite.msc = msc
	suite.impl = NewImplementerController(mac, msc)
}

func TestImplementerControllerTestSuite(t *testing.T) {
	suite.Run(t, new(ImplementerControllerTestSuite))
}

func (suite *ImplementerControllerTestSuite) TestCreateImplementerBadJson() {
	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.impl.Create(w, req)

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
    }`)
}

func (suite *ImplementerControllerTestSuite) TestCreateImplementer() {
	//Mock impl creation
	createImplResp := ImplementerResource{}
	faker.FakeData(&createImplResp)
	createImplResp.SsasGroupId = ""
	suite.mac.On("Post", mock.Anything, mock.Anything, mock.Anything).Return(apitest.ToBytes(createImplResp), nil)

	//Mock group creation
	createGroupResp := client.CreateGroupResponse{}
	faker.FakeData(&createGroupResp)
	suite.msc.On("CreateGroup", mock.Anything, mock.Anything).Return(createGroupResp, nil)
	//Mock impl update
	updateImplResp := ImplementerResource{
		ID:          createImplResp.ID,
		Name:        createImplResp.Name,
		SsasGroupId: createGroupResp.GroupID,
	}
	suite.mac.On("Put", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(apitest.ToBytes(updateImplResp), nil)

	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", strings.NewReader(apitest.ImplJSON))
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()
	suite.impl.Create(w, req)
	res := w.Result()
	var v map[string]interface{}
	json.NewDecoder(res.Body).Decode(&v)

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.Equal(suite.T(), v["name"], createImplResp.Name)
	assert.Equal(suite.T(), v["id"], createImplResp.ID)
}
