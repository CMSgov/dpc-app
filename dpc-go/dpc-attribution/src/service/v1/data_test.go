package service

import (
	"context"
	"github.com/CMSgov/dpc/attribution/middleware"
	v1 "github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/CMSgov/dpc/attribution/repository"
	"github.com/CMSgov/dpc/attribution/service"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"net/http"
	"net/http/httptest"
	"testing"
)

type MockJobRepo struct {
	mock.Mock
}

func (jr *MockJobRepo) NewJobQueueBatch(orgID string, g *v1.GroupNPIs, patientMBIs []string, details repository.BatchDetails) *v1.JobQueueBatch {
	args := jr.Called(orgID, g, patientMBIs, details)
	return args.Get(0).(*v1.JobQueueBatch)
}

func (jr *MockJobRepo) Insert(ctx context.Context, batches []v1.JobQueueBatch) (*v1.Job, error) {
	args := jr.Called(ctx, batches)
	return args.Get(0).(*v1.Job), args.Error(1)
}

func (jr *MockJobRepo) GetGroupNPIs(ctx context.Context, groupID string) (*v1.GroupNPIs, error) {
	args := jr.Called(ctx, groupID)
	return args.Get(0).(*v1.GroupNPIs), args.Error(1)
}

func (jr *MockJobRepo) IsFileValid(ctx context.Context, orgID string, fileName string) (*v1.FileInfo, error) {
	args := jr.Called(ctx, orgID, fileName)
	return args.Get(0).(*v1.FileInfo), args.Error(1)
}

type DataServiceTestSuite struct {
	suite.Suite
	jobRepo *MockJobRepo
	service service.DataService
}

func TestDataServiceTestSuite(t *testing.T) {
	suite.Run(t, new(DataServiceTestSuite))
}

func (suite *DataServiceTestSuite) SetupTest() {
	suite.jobRepo = &MockJobRepo{}
	suite.service = NewDataService(suite.jobRepo)
}

func (suite *DataServiceTestSuite) TestGetFileInfo() {
	fileName := "fileName"

	r := httptest.NewRequest(http.MethodGet, "http://blah.com", nil)
	ctx := r.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyFileName, fileName)
	r = r.WithContext(ctx)
	ctx = context.WithValue(ctx, middleware.ContextKeyOrganization, "12345")
	r = r.WithContext(ctx)
	w := httptest.NewRecorder()

	fi := v1.FileInfo{
		FileName:     fileName,
		FileLength:   0,
		FileCheckSum: nil,
	}
	suite.jobRepo.On("IsFileValid", mock.Anything, mock.Anything, mock.MatchedBy(func(key string) bool {
		return key == fileName
	})).Return(&fi, nil)

	suite.service.GetFileInfo(w, r)

	res := w.Result()

	//Everything happy
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
}
