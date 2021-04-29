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
	"strings"
	"testing"
)

type MockImplementorRepo struct {
	mock.Mock
}

func (m *MockImplementorRepo) Insert(ctx context.Context, body []byte) (*model.Implementor, error) {
	args := m.Called(ctx, body)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Implementor), args.Error(1)
}
func (m *MockImplementorRepo) FindByID(ctx context.Context, id string) (*model.Implementor, error) {
	args := m.Called(ctx, id)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.Implementor), args.Error(1)
}

type ImplementorServiceTestSuite struct {
	suite.Suite
	repo    *MockImplementorRepo
	service *ImplementorService
}

func TestImplementorServiceTestSuite(t *testing.T) {
	suite.Run(t, new(ImplementorServiceTestSuite))
}

func (suite *ImplementorServiceTestSuite) SetupTest() {
	suite.repo = &MockImplementorRepo{}
	suite.service = NewImplementorService(suite.repo)
}

func (suite *ImplementorServiceTestSuite) TestPost() {
	ja := jsonassert.New(suite.T())

	impl := model.Implementor{}
	_ = faker.FakeData(&impl)
	suite.repo.On("Insert", mock.Anything, mock.Anything).Return(&impl, nil)

	req := httptest.NewRequest("POST", "http://example.com/foo", strings.NewReader("{\"name\":\"test-name\"}"))

	w := httptest.NewRecorder()

	suite.service.Post(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	b, _ := json.Marshal(impl)
	ja.Assertf(string(resp), string(b))
}

func (suite *ImplementorServiceTestSuite) TestSaveRepoError() {
	ja := jsonassert.New(suite.T())

	suite.repo.On("Insert", mock.Anything, mock.Anything).Return(nil, errors.New("error"))

    req := httptest.NewRequest("POST", "http://example.com/foo", strings.NewReader("{\"name\":\"test-name\"}"))

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
