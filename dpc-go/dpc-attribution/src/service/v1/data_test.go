package service

import (
	"context"
	"github.com/CMSgov/dpc/attribution/middleware"
	v1 "github.com/CMSgov/dpc/attribution/model/v1"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"net/http"
	"net/http/httptest"
	"testing"
)

type DataServiceTestSuite struct {
	suite.Suite
	jobRepo *MockJobRepo
	service DataService
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
	suite.jobRepo.On("GetFileInfo", mock.Anything, mock.Anything, mock.MatchedBy(func(key string) bool {
		return key == fileName
	})).Return(&fi, nil)

	suite.service.GetFileInfo(w, r)

	res := w.Result()

	//Everything happy
	assert.Equal(suite.T(), http.StatusOK, res.StatusCode)
}
