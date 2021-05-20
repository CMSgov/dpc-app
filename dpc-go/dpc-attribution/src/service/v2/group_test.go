package v2

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/CMSgov/dpc/attribution/model/v2"
	serviceV1 "github.com/CMSgov/dpc/attribution/service/v1"
	"github.com/bxcodec/faker/v3"
	"github.com/kinbiko/jsonassert"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type MockGrpRepo struct {
	mock.Mock
}

func (m *MockGrpRepo) Insert(ctx context.Context, body []byte) (*v2.Group, error) {
	args := m.Called(ctx, body)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*v2.Group), args.Error(1)
}

type GroupServiceTestSuite struct {
	suite.Suite
	repo    *MockGrpRepo
	service *GroupService
	js      *serviceV1.JobServiceV1
}

func TestGroupServiceTestSuite(t *testing.T) {
	suite.Run(t, new(GroupServiceTestSuite))
}

func (suite *GroupServiceTestSuite) SetupTest() {
	suite.repo = &MockGrpRepo{}
	suite.service = NewGroupService(suite.repo, suite.js)
}

func (suite *GroupServiceTestSuite) TestPost() {
	ja := jsonassert.New(suite.T())

	o := v2.Group{}
	err := faker.FakeData(&o)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.repo.On("Insert", mock.Anything, mock.Anything).Return(&o, nil)

	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", nil)

	w := httptest.NewRecorder()

	suite.service.Post(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	b, _ := json.Marshal(o)
	ja.Assertf(string(resp), string(b))
}

func (suite *GroupServiceTestSuite) TestPostRepoError() {
	ja := jsonassert.New(suite.T())

	suite.repo.On("Insert", mock.Anything, mock.Anything).Return(nil, errors.New("error"))

	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", nil)

	w := httptest.NewRecorder()

	suite.service.Post(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusUnprocessableEntity, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
        "error": "Unprocessable Entity",
        "message": "error",
        "statusCode": 422
    }`)
}

func (suite *GroupServiceTestSuite) TestGetNotImplemented() {
	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Get(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}

func (suite *GroupServiceTestSuite) TestDeleteNotImplemented() {
	req := httptest.NewRequest(http.MethodDelete, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Delete(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}

func (suite *GroupServiceTestSuite) TestPutNotImplemented() {
	req := httptest.NewRequest(http.MethodPut, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Put(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}
