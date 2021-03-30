package v2

import (
	"context"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/CMSgov/dpc/attribution/model/v2"

	"github.com/bxcodec/faker"
	"github.com/kinbiko/jsonassert"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type MockRepo struct {
	mock.Mock
}

func (m *MockRepo) Insert(ctx context.Context, body []byte) (*v2.Organization, error) {
	args := m.Called(ctx, body)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*v2.Organization), args.Error(1)
}
func (m *MockRepo) FindByID(ctx context.Context, id string) (*v2.Organization, error) {
	args := m.Called(ctx, id)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*v2.Organization), args.Error(1)
}

func (m *MockRepo) DeleteByID(ctx context.Context, id string) error {
	args := m.Called(ctx, id)
	return args.Error(0)
}
func (m *MockRepo) Update(ctx context.Context, id string, body []byte) (*v2.Organization, error) {
	args := m.Called(ctx, id, body)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*v2.Organization), args.Error(1)
}

type OrganizationServiceTestSuite struct {
	suite.Suite
}

func TestOrganizationServiceTestSuite(t *testing.T) {
	suite.Run(t, new(OrganizationServiceTestSuite))
}

func (suite *OrganizationServiceTestSuite) TestGetRepoError() {
	ja := jsonassert.New(suite.T())
	mr := new(MockRepo)
	os := NewOrganizationService(mr)

	mr.On("FindByID", mock.Anything, mock.Anything).Return(nil, errors.New("error"))

	req := httptest.NewRequest("GET", "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	os.Get(w, req)

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
	mr := new(MockRepo)
	os := NewOrganizationService(mr)

	o := v2.Organization{}
	_ = faker.FakeData(&o)
	mr.On("FindByID", mock.Anything, mock.Anything).Return(&o, nil)

	req := httptest.NewRequest("GET", "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	os.Get(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	b, _ := json.Marshal(o)
	ja.Assertf(string(resp), string(b))
}

func (suite *OrganizationServiceTestSuite) TestPost() {
	ja := jsonassert.New(suite.T())
	mr := new(MockRepo)
	os := NewOrganizationService(mr)

	o := v2.Organization{}
	_ = faker.FakeData(&o)
	mr.On("Insert", mock.Anything, mock.Anything).Return(&o, nil)

	req := httptest.NewRequest("POST", "http://example.com/foo", nil)

	w := httptest.NewRecorder()

	os.Post(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	b, _ := json.Marshal(o)
	ja.Assertf(string(resp), string(b))
}

func (suite *OrganizationServiceTestSuite) TestSaveRepoError() {
	ja := jsonassert.New(suite.T())
	mr := new(MockRepo)
	os := NewOrganizationService(mr)

	mr.On("Insert", mock.Anything, mock.Anything).Return(nil, errors.New("error"))

	req := httptest.NewRequest("POST", "http://example.com/foo", nil)

	w := httptest.NewRecorder()

	os.Post(w, req)

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
	mr := new(MockRepo)
	os := NewOrganizationService(mr)

	req := httptest.NewRequest("POST", "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	mr.On("DeleteByID", mock.Anything, mock.Anything).Return(errors.New("error")).Once()
	os.Delete(w, req)

	res := w.Result()

	//Repo not happy
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusNotFound, res.StatusCode)

	w = httptest.NewRecorder()

	mr.On("DeleteByID", mock.Anything, mock.Anything).Return(nil).Once()
	os.Delete(w, req)

	res = w.Result()

	//Repo happy
	assert.Equal(suite.T(), http.StatusNoContent, res.StatusCode)
}

func (suite *OrganizationServiceTestSuite) TestPut() {
	ja := jsonassert.New(suite.T())
	mr := new(MockRepo)
	os := NewOrganizationService(mr)

	req := httptest.NewRequest("PUT", "http://example.com/foo", nil)
	w := httptest.NewRecorder()

	os.Put(w, req)

	res := w.Result()

	//No organization id
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusBadRequest, res.StatusCode)

	ctx := req.Context()
	ctx = context.WithValue(ctx, ContextKeyOrganization, "12345")
	req = req.WithContext(ctx)

	w = httptest.NewRecorder()

	mr.On("Update", mock.Anything, mock.Anything, mock.Anything).Return(nil, errors.New("test")).Once()
	os.Put(w, req)

	res = w.Result()

	//Repo returns an error
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusUnprocessableEntity, res.StatusCode)

	o := v2.Organization{}
	_ = faker.FakeData(&o)
	w = httptest.NewRecorder()

	mr.On("Update", mock.Anything, mock.Anything, mock.Anything).Return(&o, nil).Once()
	os.Put(w, req)

	res = w.Result()

	//Everything happy
	assert.Equal(suite.T(), "application/json; charset=UTF-8", res.Header.Get("Content-Type"))
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	b, _ := json.Marshal(o)
	ja.Assertf(string(resp), string(b))
}
