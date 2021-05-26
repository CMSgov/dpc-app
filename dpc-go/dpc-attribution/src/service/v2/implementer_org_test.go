package v2

import (
	"context"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/CMSgov/dpc/attribution/middleware"
	v2 "github.com/CMSgov/dpc/attribution/model/v2"
	"github.com/bxcodec/faker/v3"
	"github.com/kinbiko/jsonassert"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type MockImplementerOrgRepo struct {
	mock.Mock
}

func (m *MockImplementerOrgRepo) Insert(ctx context.Context, implId string, orgId string, status v2.ImplOrgStatus) (*v2.ImplementerOrgRelation, error) {
	args := m.Called(ctx, implId, orgId, status)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*v2.ImplementerOrgRelation), args.Error(1)
}
func (m *MockImplementerOrgRepo) FindRelation(ctx context.Context, implId string, orgId string) (*v2.ImplementerOrgRelation, error) {
	args := m.Called(ctx, implId, orgId)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*v2.ImplementerOrgRelation), args.Error(1)
}

type ImplementerOrgServiceTestSuite struct {
	suite.Suite
	implRepo    *MockImplementerRepo
	orgRepo     *MockOrgRepo
	implOrgRepo *MockImplementerOrgRepo
	service     *ImplementerOrgService
}

func TestImplementerOrgServiceTestSuite(t *testing.T) {
	suite.Run(t, new(ImplementerOrgServiceTestSuite))
}

func (suite *ImplementerOrgServiceTestSuite) SetupTest() {
	suite.implRepo = &MockImplementerRepo{}
	suite.orgRepo = &MockOrgRepo{}
	suite.implOrgRepo = &MockImplementerOrgRepo{}
	suite.service = NewImplementerOrgService(suite.implRepo, suite.orgRepo, suite.implOrgRepo, true)
}

func (suite *ImplementerOrgServiceTestSuite) TestPost() {

	implOrg := v2.ImplementerOrgRelation{}
	err := faker.FakeData(&implOrg)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	implOrg.Status = v2.Active
	suite.implOrgRepo.On("Insert", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(&implOrg, nil)

	impl := v2.Implementer{}
	err = faker.FakeData(&impl)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.implRepo.On("FindByID", mock.Anything, mock.Anything).Return(&impl, nil)

	suite.orgRepo.On("FindByNPI", mock.Anything, mock.Anything).Return(nil, nil)

	org := v2.Organization{}
	err = faker.FakeData(&org)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.orgRepo.On("Insert", mock.Anything, mock.Anything).Return(&org, nil)

	rel := v2.ImplementerOrgRelation{}
	err = faker.FakeData(&rel)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.implOrgRepo.On("FindRelation", mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	req := httptest.NewRequest("POST", "http://example.com/foo", strings.NewReader("{\"npi\":\"00001\"}"))
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyImplementer, implOrg.ImplementerID)
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	suite.service.Post(w, req)
	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
}

func (suite *ImplementerOrgServiceTestSuite) TestSaveRepoError() {
	ja := jsonassert.New(suite.T())

	implOrg := v2.ImplementerOrgRelation{}
	err := faker.FakeData(&implOrg)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	implOrg.Status = v2.Active
	suite.implOrgRepo.On("Insert", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, errors.New("error"))

	impl := v2.Implementer{}
	err = faker.FakeData(&impl)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.implRepo.On("FindByID", mock.Anything, mock.Anything).Return(&impl, nil)

	org := v2.Organization{}
	err = faker.FakeData(&org)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.orgRepo.On("FindByNPI", mock.Anything, mock.Anything).Return(&org, nil)

	rel := v2.ImplementerOrgRelation{}
	err = faker.FakeData(&rel)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.implOrgRepo.On("FindRelation", mock.Anything, mock.Anything, mock.Anything).Return(nil, nil)

	req := httptest.NewRequest("POST", "http://example.com/foo", strings.NewReader("{\"npi\":\"00001\"}"))
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyImplementer, implOrg.ImplementerID)
	req = req.WithContext(ctx)
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

func (suite *ImplementerOrgServiceTestSuite) TestGetNotImplemented() {
	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Get(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}

func (suite *ImplementerOrgServiceTestSuite) TestDeleteNotImplemented() {
	req := httptest.NewRequest(http.MethodDelete, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Delete(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}

func (suite *ImplementerOrgServiceTestSuite) TestPutNotImplemented() {
	req := httptest.NewRequest(http.MethodPut, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Put(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}

func (suite *ImplementerOrgServiceTestSuite) TestExportNotImplemented() {
	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Export(w, req)
	res := w.Result()
	assert.Equal(suite.T(), http.StatusNotImplemented, res.StatusCode)
}
