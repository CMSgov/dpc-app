package v2

import (
	"context"
	"encoding/json"
	"github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/bxcodec/faker"
	"github.com/kinbiko/jsonassert"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"
)

type MockOrgRepo struct {
	mock.Mock
}

func (m *MockOrgRepo) Insert(ctx context.Context, body []byte) (*model.Organization, error) {
	args := m.Called(ctx, body)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Organization), args.Error(1)
}
func (m *MockOrgRepo) FindByID(ctx context.Context, id string) (*model.Organization, error) {
	args := m.Called(ctx, id)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Organization), args.Error(1)
}

func (m *MockOrgRepo) FindByNPI(ctx context.Context, npi string) (*model.Organization, error) {
	args := m.Called(ctx, npi)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Organization), args.Error(1)
}

func (m *MockOrgRepo) DeleteByID(ctx context.Context, id string) error {
	args := m.Called(ctx, id)
	return args.Error(0)
}
func (m *MockOrgRepo) Update(ctx context.Context, id string, body []byte) (*model.Organization, error) {
	args := m.Called(ctx, id, body)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Organization), args.Error(1)
}

type OrganizationServiceTestSuite struct {
	suite.Suite
	repo    *MockOrgRepo
	service *OrganizationService
}

func TestOrganizationServiceTestSuite(t *testing.T) {
	suite.Run(t, new(OrganizationServiceTestSuite))
}

func (suite *OrganizationServiceTestSuite) SetupTest() {
	suite.repo = &MockOrgRepo{}
	suite.service = NewOrganizationService(suite.repo)
}

func (suite *OrganizationServiceTestSuite) TestGetRepoError() {
	ja := jsonassert.New(suite.T())

	suite.repo.On("FindByID", mock.Anything, mock.Anything).Return(nil, errors.New("error"))

	req := httptest.NewRequest("GET", "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.service.Get(w, req)

	res := w.Result()

	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusNotFound, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), `
    {
        "error": "Not Found",
        "message": "error",
        "statusCode": 404
    }`)
}

func (suite *OrganizationServiceTestSuite) TestGet() {
	ja := jsonassert.New(suite.T())

	o := model.Organization{}
	_ = faker.FakeData(&o)
	suite.repo.On("FindByID", mock.Anything, mock.Anything).Return(&o, nil)

	req := httptest.NewRequest("GET", "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.service.Get(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	b, _ := json.Marshal(o)
	ja.Assertf(string(resp), string(b))
}

func (suite *OrganizationServiceTestSuite) TestPost() {
	ja := jsonassert.New(suite.T())

	o := model.Organization{}
	_ = faker.FakeData(&o)
	suite.repo.On("Insert", mock.Anything, mock.Anything).Return(&o, nil)

	req := httptest.NewRequest("POST", "http://example.com/foo", nil)

	w := httptest.NewRecorder()

	suite.service.Post(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	b, _ := json.Marshal(o)
	ja.Assertf(string(resp), string(b))
}

func (suite *OrganizationServiceTestSuite) TestSaveRepoError() {
	ja := jsonassert.New(suite.T())

	suite.repo.On("Insert", mock.Anything, mock.Anything).Return(nil, errors.New("error"))

	req := httptest.NewRequest("POST", "http://example.com/foo", nil)

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

func (suite *OrganizationServiceTestSuite) TestDelete() {
	req := httptest.NewRequest("POST", "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	suite.repo.On("DeleteByID", mock.Anything, mock.Anything).Return(errors.New("error")).Once()
	suite.service.Delete(w, req)

	res := w.Result()

	//Repo not happy
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusNotFound, res.StatusCode)

	w = httptest.NewRecorder()

	suite.repo.On("DeleteByID", mock.Anything, mock.Anything).Return(nil).Once()
	suite.service.Delete(w, req)

	res = w.Result()

	//Repo happy
	assert.Equal(suite.T(), http.StatusNoContent, res.StatusCode)
}

func (suite *OrganizationServiceTestSuite) TestPut() {
	ja := jsonassert.New(suite.T())

	req := httptest.NewRequest("PUT", "http://example.com/foo", nil)
	w := httptest.NewRecorder()

	suite.service.Put(w, req)

	res := w.Result()

	//No organization id
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)

	w = httptest.NewRecorder()

	suite.repo.On("Update", mock.Anything, mock.Anything, mock.Anything).Return(nil, errors.New("test")).Once()
	suite.service.Put(w, req)

	res = w.Result()

	//Repo returns an error
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusUnprocessableEntity, res.StatusCode)

	o := model.Organization{}
	_ = faker.FakeData(&o)
	w = httptest.NewRecorder()

	suite.repo.On("Update", mock.Anything, mock.Anything, mock.Anything).Return(&o, nil).Once()
	suite.service.Put(w, req)

	res = w.Result()

	//Everything happy
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	b, _ := json.Marshal(o)
	ja.Assertf(string(resp), string(b))
}
