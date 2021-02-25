package v2

import (
	"context"
	"encoding/json"
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

type MockRepo struct {
	mock.Mock
}

func (m *MockRepo) Insert(ctx context.Context, body []byte) (*model.Organization, error) {
	args := m.Called(ctx, body)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Organization), args.Error(1)
}
func (m *MockRepo) FindByID(ctx context.Context, id string) (*model.Organization, error) {
	args := m.Called(ctx, id)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Organization), args.Error(1)
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

	o := model.Organization{}
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

func (suite *OrganizationServiceTestSuite) TestSave() {
	ja := jsonassert.New(suite.T())
	mr := new(MockRepo)
	os := NewOrganizationService(mr)

	o := model.Organization{}
	_ = faker.FakeData(&o)
	mr.On("Insert", mock.Anything, mock.Anything).Return(&o, nil)

	req := httptest.NewRequest("POST", "http://example.com/foo", nil)

	w := httptest.NewRecorder()

	os.Save(w, req)

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

	os.Save(w, req)

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
