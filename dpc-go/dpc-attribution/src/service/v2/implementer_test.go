package v2

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/CMSgov/dpc/attribution/model/v2"
	"github.com/bxcodec/faker/v3"
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
	err := faker.FakeData(&impl)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.repo.On("Insert", mock.Anything, mock.Anything).Return(&impl, nil)

	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", strings.NewReader(`{"name":"test-name"}`))

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

	req := httptest.NewRequest(http.MethodPost, "http://example.com/foo", strings.NewReader("{\"name\":\"test-name\"}"))

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

func (suite *ImplementerServiceTestSuite) TestGetNotImplemented() {
	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Get(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}

func (suite *ImplementerServiceTestSuite) TestDeleteNotImplemented() {
	req := httptest.NewRequest(http.MethodDelete, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Delete(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}

func (suite *ImplementerServiceTestSuite) TestPutNotImplemented() {
	req := httptest.NewRequest(http.MethodPut, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Put(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}

func (suite *ImplementerServiceTestSuite) TestExportNotImplemented() {
	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Export(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}
