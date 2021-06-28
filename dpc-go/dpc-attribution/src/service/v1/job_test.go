package service

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/CMSgov/dpc/attribution/conf"
	middleware2 "github.com/CMSgov/dpc/attribution/middleware"
	"github.com/CMSgov/dpc/attribution/model/v1"
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

func (m *MockJobRepo) Insert(ctx context.Context, orgID string, b []v1.BatchRequest) (*string, error) {
	args := m.Called(ctx, orgID, b)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*string), args.Error(1)
}

func (m *MockJobRepo) FindBatchesByJobID(id string, orgID string) ([]v1.JobQueueBatch, error) {
	args := m.Called(id, orgID)
	return args.Get(0).([]v1.JobQueueBatch), args.Error(1)
}

func (m *MockJobRepo) FindBatchFilesByBatchID(id string) ([]v1.JobQueueBatchFile, error) {
	args := m.Called(id)
	return args.Get(0).([]v1.JobQueueBatchFile), args.Error(1)
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
	jqb     *v1.JobQueueBatch
}

func TestJobServiceV1TestSuite(t *testing.T) {
	suite.Run(t, new(JobServiceV1TestSuite))
}

func (suite *JobServiceV1TestSuite) SetupTest() {
	conf.NewConfig("../../../configs")
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

	suite.jqb = &jqb
	suite.pr.On("FindMBIsByGroupID", mock.Anything).Return(m, nil)
	suite.pr.On("GetGroupNPIs", mock.Anything, mock.Anything).Return(&g, nil)
}

func (suite *JobServiceV1TestSuite) TestExport() {
	ja := jsonassert.New(suite.T())

	id := "12345"
	suite.jr.On("Insert", mock.Anything, mock.Anything, mock.Anything).Return(&id, nil)

	req := httptest.NewRequest("GET", "http://example.com/v2/Group/9876/$export", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, faker.UUIDHyphenated())
	req.Header.Set(middleware2.FwdHeader, faker.IPv4())
	req.Header.Set(middleware2.RequestURLHeader, faker.URL())
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()
	suite.service.Export(w, req)

	res := w.Result()

	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)

	resp, _ := ioutil.ReadAll(res.Body)

	ja.Assertf(string(resp), id)
}

func (suite *JobServiceV1TestSuite) TestExportRepoError() {
	ja := jsonassert.New(suite.T())

	suite.jr.On("Insert", mock.Anything, mock.Anything, mock.Anything).Return(nil, errors.New("error"))

	req := httptest.NewRequest("GET", "http://example.com/v2/Group/9876/$export", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyGroup, faker.UUIDHyphenated())
	req.Header.Set(middleware2.FwdHeader, faker.IPv4())
	req.Header.Set(middleware2.RequestURLHeader, faker.URL())
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

func (suite *JobServiceV1TestSuite) TestGetBatchesAndFiles() {
	req := httptest.NewRequest("GET", "http://doesnotmatter.com", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware2.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyJobID, "54321")
	req = req.WithContext(ctx)

	suite.jr.On("FindBatchesByJobID", mock.MatchedBy(func(passedInJobID string) bool {
		return passedInJobID == "54321"
	}), mock.MatchedBy(func(passedInOrgID string) bool {
		return passedInOrgID == "12345"
	})).Return([]v1.JobQueueBatch{*suite.jqb}, nil)

	suite.jr.On("FindBatchFilesByBatchID", mock.MatchedBy(func(passedInBatchID string) bool {
		return passedInBatchID == suite.jqb.BatchID
	})).Return([]v1.JobQueueBatchFile{
		{
			ResourceType: nil,
			BatchID:      suite.jqb.BatchID,
			Sequence:     0,
			FileName:     "testFileName",
			Count:        1,
			Checksum:     nil,
			FileLength:   1234,
		},
	}, nil)

	w := httptest.NewRecorder()
	suite.service.BatchesAndFiles(w, req)
	res := w.Result()

	resp, _ := ioutil.ReadAll(res.Body)
	var batchesAndFiles []v1.BatchAndFiles
	_ = json.Unmarshal(resp, &batchesAndFiles)

	assert.NotNil(suite.T(), batchesAndFiles)
	assert.Len(suite.T(), batchesAndFiles, 1)
	assert.Equal(suite.T(), "testFileName", batchesAndFiles[0].Files[0].FileName)
}
