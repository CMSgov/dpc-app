package v2

import (
	"context"
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
	"strings"
	"testing"
)

type MockImplementerOrgRepo struct {
	mock.Mock
}

func (m *MockImplementerOrgRepo) Insert(ctx context.Context, implId string, orgId string, status model.ImplOrgStatus) (*model.ImplementerOrgRelation, error) {
	args := m.Called(ctx, implId, orgId, status)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.ImplementerOrgRelation), args.Error(1)
}
func (m *MockImplementerOrgRepo) FindRelation(ctx context.Context, implId string, orgId string) (*model.ImplementerOrgRelation, error) {
	args := m.Called(ctx, implId, orgId)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.ImplementerOrgRelation), args.Error(1)
}

func (m *MockImplementerOrgRepo) FindManagedOrgs(ctx context.Context, implId string) ([]model.ImplementerOrgRelation, error) {
    args := m.Called(ctx, implId)
    if args.Get(0) == nil {
        return nil, args.Error(1)
    }
    return args.Get(0).([]model.ImplementerOrgRelation), args.Error(1)
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

	implOrg := model.ImplementerOrgRelation{}
	_ = faker.FakeData(&implOrg)
	implOrg.Status = model.Active
	suite.implOrgRepo.On("Insert", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(&implOrg, nil)

	impl := model.Implementer{}
	_ = faker.FakeData(&impl)
	suite.implRepo.On("FindByID", mock.Anything, mock.Anything).Return(&impl, nil)

	suite.orgRepo.On("FindByNPI", mock.Anything, mock.Anything).Return(nil, nil)

	org := model.Organization{}
	_ = faker.FakeData(&org)
	suite.orgRepo.On("Insert", mock.Anything, mock.Anything).Return(&org, nil)

	rel := model.ImplementerOrgRelation{}
	_ = faker.FakeData(&rel)
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

func (suite *ImplementerOrgServiceTestSuite) TestGetOrgs() {

    implOrg := model.ImplementerOrgRelation{}
    _ = faker.FakeData(&implOrg)
    implOrg.Status = model.Active

    implOrg2 := model.ImplementerOrgRelation{}
    _ = faker.FakeData(&implOrg)
    implOrg.Status = model.Active

    orgs := []model.ImplementerOrgRelation{implOrg, implOrg2}
    suite.implOrgRepo.On("FindManagedOrgs", mock.Anything, mock.Anything).Return(orgs, nil)

    impl := model.Implementer{}
    _ = faker.FakeData(&impl)
    suite.implRepo.On("FindByID", mock.Anything, mock.Anything).Return(&impl, nil)


    req := httptest.NewRequest("GET", "http://example.com/foo",  strings.NewReader(""))
    ctx := req.Context()
    ctx = context.WithValue(ctx, middleware.ContextKeyImplementer, implOrg.ImplementerID)
    req = req.WithContext(ctx)
    w := httptest.NewRecorder()

    suite.service.Get(w, req)
    res := w.Result()

    resp, _ := ioutil.ReadAll(res.Body)
    respS := string(resp)

    assert.Contains(suite.T(),respS,implOrg.OrganizationID)
    assert.Contains(suite.T(),respS,implOrg2.OrganizationID)
    assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
}

func (suite *ImplementerOrgServiceTestSuite) TestSaveRepoError() {
	ja := jsonassert.New(suite.T())

	implOrg := model.ImplementerOrgRelation{}
	_ = faker.FakeData(&implOrg)
	implOrg.Status = model.Active
	suite.implOrgRepo.On("Insert", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, errors.New("error"))

	impl := model.Implementer{}
	_ = faker.FakeData(&impl)
	suite.implRepo.On("FindByID", mock.Anything, mock.Anything).Return(&impl, nil)

	org := model.Organization{}
	_ = faker.FakeData(&org)
	suite.orgRepo.On("FindByNPI", mock.Anything, mock.Anything).Return(&org, nil)

	rel := model.ImplementerOrgRelation{}
	_ = faker.FakeData(&rel)
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
