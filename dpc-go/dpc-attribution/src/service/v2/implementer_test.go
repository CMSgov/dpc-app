package v2

import (
	"context"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/CMSgov/dpc/attribution/model/v2"
	"github.com/bxcodec/faker"
	"github.com/kinbiko/jsonassert"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type MockImplementerRepo struct {
	mock.Mock
}

func (m *MockImplementerRepo) Insert(ctx context.Context, body []byte) (*v2.Implementer, error) {
	args := m.Called(ctx, body)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*v2.Implementer), args.Error(1)
}
func (m *MockImplementerRepo) FindByID(ctx context.Context, id string) (*v2.Implementer, error) {
	args := m.Called(ctx, id)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*v2.Implementer), args.Error(1)
}

type ImplementerServiceTestSuite struct {
	suite.Suite
	repo    *MockImplementerRepo
	service *ImplementerService
}

func TestImplementerServiceTestSuite(t *testing.T) {
	suite.Run(t, new(ImplementerServiceTestSuite))
}

func (suite *ImplementerServiceTestSuite) SetupTest() {
	suite.repo = &MockImplementerRepo{}
	suite.service = NewImplementerService(suite.repo)
}

func (suite *ImplementerServiceTestSuite) TestPost() {
	ja := jsonassert.New(suite.T())

	impl := v2.Implementer{}
	_ = faker.FakeData(&impl)
	suite.repo.On("Insert", mock.Anything, mock.Anything).Return(&impl, nil)

	req := httptest.NewRequest("POST", "http://example.com/foo", strings.NewReader(`{"name":"test-name"}`))

	w := httptest.NewRecorder()

	suite.service.Post(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	b, _ := json.Marshal(impl)
	ja.Assertf(string(resp), string(b))
}

func (suite *ImplementerServiceTestSuite) TestSaveRepoError() {
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
