package service

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/attributiontest"
	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/CMSgov/dpc/attribution/model"
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

func (m *MockGrpRepo) Insert(ctx context.Context, body []byte) (*model.Group, error) {
	args := m.Called(ctx, body)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Group), args.Error(1)
}

func (m *MockGrpRepo) FindByID(ctx context.Context, id string) (*model.Group, error) {
	args := m.Called(ctx, id)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Group), args.Error(1)
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

	o := model.Group{}
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

func (suite *GroupServiceTestSuite) TestGet() {
	g := attributiontest.GroupResponse()
	suite.repo.On("FindByID", mock.Anything, mock.MatchedBy(func(groupID string) bool {
		return groupID == "54321"
	})).Return(g, nil)

	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, "54321")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()
	suite.service.Get(w, req)
	res := w.Result()

	b, _ := ioutil.ReadAll(res.Body)
	assert.NotNil(suite.T(), b)
}

func (suite *GroupServiceTestSuite) TestGetError() {
	ja := jsonassert.New(suite.T())
	suite.repo.On("FindByID", mock.Anything, mock.MatchedBy(func(groupID string) bool {
		return groupID == "54321"
	})).Return(nil, errors.New("error"))

	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, "54321")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()
	suite.service.Get(w, req)
	res := w.Result()

	b, _ := ioutil.ReadAll(res.Body)
	assert.Equal(suite.T(), http.StatusUnprocessableEntity, res.StatusCode)
	ja.Assertf(string(b), `
    {
        "error": "Unprocessable Entity",
        "message": "error",
        "statusCode": 422
    }`)
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
