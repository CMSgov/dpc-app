package service

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/CMSgov/dpc/attribution/service"
	"github.com/bxcodec/faker/v3"
	"github.com/kinbiko/jsonassert"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
)

type MockJobRepo struct {
	mock.Mock
}

func (m *MockJobRepo) NewJobQueueBatch(id string, g *v1.GroupNPIs, p []string, d repository.BatchDetails) *v1.JobQueueBatch {
	args := m.Called(id, g, p, d)
	return args.Get(0).(*v1.JobQueueBatch)
}

func (m *MockJobRepo) Insert(ctx context.Context, b []v1.JobQueueBatch) (*v1.Job, error) {
	args := m.Called(ctx, b)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*v1.Job), args.Error(1)
}

func (m *MockJobRepo) GetFileInfo(ctx context.Context, orgID string, fileName string) (*v1.FileInfo, error) {
	args := m.Called(ctx, orgID, fileName)
	return args.Get(0).(*v1.FileInfo), args.Error(1)
}

type MockPatientRepo struct {
	mock.Mock
}

func (m *MockPatientRepo) FindMBIsByGroupID(id string) ([]string, error) {
	args := m.Called(id)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).([]string), args.Error(1)
}

func (m *MockPatientRepo) GetGroupNPIs(ctx context.Context, id string) (*v1.GroupNPIs, error) {
	args := m.Called(ctx, id)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*v1.GroupNPIs), args.Error(1)
}

type JobServiceV1TestSuite struct {
	suite.Suite
	jr      *MockJobRepo
	pr      *MockPatientRepo
	service service.JobService
	job     *v1.Job
}

func TestJobServiceV1TestSuite(t *testing.T) {
	suite.Run(t, new(JobServiceV1TestSuite))
}

func (suite *JobServiceV1TestSuite) SetupTest() {
	suite.jr = &MockJobRepo{}
	suite.pr = &MockPatientRepo{}
	suite.service = NewJobService(suite.pr, suite.jr)
	m := []string{faker.UUIDDigit(), faker.UUIDDigit(), faker.UUIDDigit()}
	g := v1.GroupNPIs{}
	err := faker.FakeData(&g)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	jqb := v1.JobQueueBatch{}
	err = faker.FakeData(&jqb)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	j := v1.Job{}
	err = faker.FakeData(&j)
	if err != nil {
		fmt.Printf("ERR %v\n", err)
	}
	suite.job = &j
	suite.pr.On("FindMBIsByGroupID", mock.Anything).Return(m, nil)
	suite.pr.On("GetGroupNPIs", mock.Anything, mock.Anything).Return(&g, nil)
	suite.jr.On("NewJobQueueBatch", mock.Anything, mock.Anything, mock.Anything, mock.Anything).Return(&jqb)
}

func (suite *JobServiceV1TestSuite) TestExport() {
	ja := jsonassert.New(suite.T())

	suite.jr.On("Insert", mock.Anything, mock.Anything).Return(suite.job, nil)

	req := httptest.NewRequest("GET", "http://example.com/v2/Group/9876/$export", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, faker.UUIDHyphenated())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()
	suite.service.Export(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	b, _ := json.Marshal(v1.Job{ID: suite.job.ID})
	ja.Assertf(string(resp), string(b))
}

func (suite *JobServiceV1TestSuite) TestExportRepoError() {
	ja := jsonassert.New(suite.T())

	suite.jr.On("Insert", mock.Anything, mock.Anything).Return(nil, errors.New("error"))

	req := httptest.NewRequest("GET", "http://example.com/v2/Group/9876/$export", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, faker.UUIDHyphenated())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, middleware2.ContextKeyRequestingIP, faker.IPv4())
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.service.Export(w, req)

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
