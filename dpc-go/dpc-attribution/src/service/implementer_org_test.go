package service

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/model"
	"github.com/bxcodec/faker/v3"
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

func (m *MockImplementerOrgRepo) Update(ctx context.Context, implId string, orgId string, sysId string) (*model.ImplementerOrgRelation, error) {
	args := m.Called(ctx, implId, orgId, sysId)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*model.ImplementerOrgRelation), args.Error(1)
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
	err := faker.FakeData(&implOrg)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	implOrg.Status = model.Active
	suite.implOrgRepo.On("Insert", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(&implOrg, nil)

	impl := model.Implementer{}
	err = faker.FakeData(&impl)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.implRepo.On("FindByID", mock.Anything, mock.Anything).Return(&impl, nil)

	suite.orgRepo.On("FindByNPI", mock.Anything, mock.Anything).Return(nil, nil)

	org := model.Organization{}
	err = faker.FakeData(&org)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.orgRepo.On("Insert", mock.Anything, mock.Anything).Return(&org, nil)

	rel := model.ImplementerOrgRelation{}
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

	b, _ := ioutil.ReadAll(res.Body)

	var response map[string]string
	_ = json.Unmarshal(b, &response)

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.Equal(suite.T(), "Active", response["status"])
	assert.NotNil(suite.T(), response["org_id"])

	req = httptest.NewRequest("POST", "http://example.com/foo", strings.NewReader("{\"npi\":\"00001\"}"))
	ctx = req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyImplementer, implOrg.ImplementerID)
	req = req.WithContext(ctx)
	ctx = context.WithValue(ctx, middleware.ContextKeyOrganization, response["org_id"])
	req = req.WithContext(ctx)
	w = httptest.NewRecorder()

	rel.Status = model.Active
	suite.implOrgRepo.On("Update", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(&rel, nil)

	suite.service.Put(w, req)

	res = w.Result()
	b, _ = ioutil.ReadAll(res.Body)
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
	assert.Equal(suite.T(), "Active", response["status"])
}

func (suite *ImplementerOrgServiceTestSuite) TestGetOrgs() {

	implOrg := model.ImplementerOrgRelation{}
	_ = faker.FakeData(&implOrg)
	implOrg.Status = model.Active

	implOrg2 := model.ImplementerOrgRelation{}
	_ = faker.FakeData(&implOrg)
	implOrg.Status = model.Active

	relations := []model.ImplementerOrgRelation{implOrg, implOrg2}
	suite.implOrgRepo.On("FindManagedOrgs", mock.Anything, mock.Anything).Return(relations, nil)

	impl := model.Implementer{}
	_ = faker.FakeData(&impl)
	suite.implRepo.On("FindByID", mock.Anything, mock.Anything).Return(&impl, nil)

	org1 := model.Organization{}
	inf1 := `{
	"resourceType": "Organization",
          "identifier": [
               {
                   "system": "http://hl7.org/fhir/sid/us-npi",
                   "value": "00010"
               }
          ],
          "name": "Some org name"}`
	var org1Info model.Info
	err := json.Unmarshal([]byte(inf1), &org1Info)
	assert.NoError(suite.T(), err)
	org1.Info = org1Info

	suite.orgRepo.On("FindByID", mock.Anything, mock.Anything).Return(&org1, nil)

	req := httptest.NewRequest("GET", "http://example.com/foo", strings.NewReader(""))
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyImplementer, implOrg.ImplementerID)
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	suite.service.Get(w, req)
	res := w.Result()

	resp, _ := ioutil.ReadAll(res.Body)
	respS := string(resp)

	assert.Contains(suite.T(), respS, implOrg.OrganizationID)
	assert.Contains(suite.T(), respS, implOrg2.OrganizationID)
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
}

func (suite *ImplementerOrgServiceTestSuite) TestSaveRepoError() {
	ja := jsonassert.New(suite.T())

	implOrg := model.ImplementerOrgRelation{}
	err := faker.FakeData(&implOrg)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	implOrg.Status = model.Active
	suite.implOrgRepo.On("Insert", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(nil, errors.New("error"))

	impl := model.Implementer{}
	err = faker.FakeData(&impl)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.implRepo.On("FindByID", mock.Anything, mock.Anything).Return(&impl, nil)

	org := model.Organization{}
	err = faker.FakeData(&org)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.orgRepo.On("FindByNPI", mock.Anything, mock.Anything).Return(&org, nil)

	rel := model.ImplementerOrgRelation{}
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

func (suite *ImplementerOrgServiceTestSuite) TestDeleteNotImplemented() {
	req := httptest.NewRequest(http.MethodDelete, "http://example.com/foo", nil)
	w := httptest.NewRecorder()
	suite.service.Delete(w, req)
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
