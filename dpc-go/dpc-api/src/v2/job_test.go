package v2

import (
	"context"
	"encoding/json"
	"github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/model"
	"github.com/bxcodec/faker/v3"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

type MockJobClient struct {
	mock.Mock
}

func (mjc *MockJobClient) Status(ctx context.Context, jobID string) ([]byte, error) {
	args := mjc.Called(ctx, jobID)
	return args.Get(0).([]byte), args.Error(1)
}

type JobControllerTestSuite struct {
	suite.Suite
	job JobController
	mjc *MockJobClient
}

func (suite *JobControllerTestSuite) SetupTest() {
	mjc := new(MockJobClient)
	suite.mjc = mjc
	suite.job = NewJobController(mjc)
}

func TestJobControllerTestSuite(t *testing.T) {
	suite.Run(t, new(JobControllerTestSuite))
}

func (suite *JobControllerTestSuite) TestGetMixedResources() {
	jobID := "12345"
	req := httptest.NewRequest(http.MethodGet, "http://blah.com", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyJobID, jobID)
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	fi := getCompletedBatchAndFiles()
	b, _ := json.Marshal(fi)
	suite.mjc.On("Status", mock.Anything, mock.MatchedBy(func(key string) bool {
		return key == jobID
	})).Return(b, nil)

	suite.job.Status(w, req)

	resp := w.Result()

	b, _ = ioutil.ReadAll(resp.Body)

	var status model.Status
	_ = json.Unmarshal(b, &status)

	assert.Equal(suite.T(), http.StatusOK, resp.StatusCode)
	assert.Equal(suite.T(), 1, len(status.Output))
	assert.Equal(suite.T(), 1, len(status.Error))
	assert.True(suite.T(), status.RequiresAccessToken)
	assert.NotNil(suite.T(), status.TransactionTime)
	assert.NotNil(suite.T(), status.Request)
}

func (suite *JobControllerTestSuite) TestInProgress() {
	jobID := "12345"
	req := httptest.NewRequest(http.MethodGet, "http://blah.com", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyJobID, jobID)
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	fi := getInProgressBatchAndFiles()
	b, _ := json.Marshal(fi)
	suite.mjc.On("Status", mock.Anything, mock.MatchedBy(func(key string) bool {
		return key == jobID
	})).Return(b, nil)

	suite.job.Status(w, req)

	resp := w.Result()

	b, _ = ioutil.ReadAll(resp.Body)

	assert.Equal(suite.T(), http.StatusAccepted, resp.StatusCode)
	assert.Equal(suite.T(), 0, len(b))
	assert.Equal(suite.T(), "RUNNING: 50.00%", resp.Header.Get("X-Progress"))
}

func (suite *JobControllerTestSuite) TestExpired() {
	jobID := "12345"
	req := httptest.NewRequest(http.MethodGet, "http://blah.com", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyJobID, jobID)
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	fi := getExpiredBatchAndFiles()
	b, _ := json.Marshal(fi)
	suite.mjc.On("Status", mock.Anything, mock.MatchedBy(func(key string) bool {
		return key == jobID
	})).Return(b, nil)

	suite.job.Status(w, req)

	resp := w.Result()

	b, _ = ioutil.ReadAll(resp.Body)

	assert.Equal(suite.T(), http.StatusGone, resp.StatusCode)
	assert.Equal(suite.T(), 0, len(b))
}

func (suite *JobControllerTestSuite) TestFailed() {
	jobID := "12345"
	req := httptest.NewRequest(http.MethodGet, "http://blah.com", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.ContextKeyJobID, jobID)
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()

	fi := getFailedBatchAndFiles()
	b, _ := json.Marshal(fi)
	suite.mjc.On("Status", mock.Anything, mock.MatchedBy(func(key string) bool {
		return key == jobID
	})).Return(b, nil)

	suite.job.Status(w, req)

	resp := w.Result()

	b, _ = ioutil.ReadAll(resp.Body)
	o, _ := fhir.UnmarshalOperationOutcome(b)

	assert.Equal(suite.T(), http.StatusInternalServerError, resp.StatusCode)
	assert.NotNil(suite.T(), o)
}

func getExpiredBatchAndFiles() []model.BatchAndFiles {
	t := time.Now().Add(-time.Duration(24) * time.Hour)
	bf1 := new(model.BatchAndFiles)
	_ = faker.FakeData(&bf1)
	bf1.Batch.Status = "COMPLETED"
	bf1.Batch.CompleteTime = &t
	bf1.Batch.SubmitTime = time.Now()
	return []model.BatchAndFiles{*bf1}
}

func getInProgressBatchAndFiles() []model.BatchAndFiles {
	t := time.Now()
	bf1 := new(model.BatchAndFiles)
	_ = faker.FakeData(&bf1)
	bf1.Batch.TotalPatients = 100
	bf1.Batch.PatientsProcessed = 50
	bf1.Batch.Status = "RUNNING"
	bf1.Batch.SubmitTime = t
	return []model.BatchAndFiles{*bf1}
}

func getFailedBatchAndFiles() []model.BatchAndFiles {
	t := time.Now()
	bf1 := new(model.BatchAndFiles)
	_ = faker.FakeData(&bf1)
	bf1.Batch.CompleteTime = &t
	bf1.Batch.SubmitTime = t
	bf1.Batch.Status = "FAILED"
	bf1.Files = []model.BatchFile{bf1.Files[0]}
	for idx, _ := range bf1.Files {
		bf1.Files[idx].ResourceType = "OperationOutcome"
	}
	bf2 := new(model.BatchAndFiles)
	_ = faker.FakeData(&bf2)
	bf2.Batch.CompleteTime = &t
	bf2.Batch.SubmitTime = t
	bf2.Batch.Status = "COMPLETED"
	bf2.Files = []model.BatchFile{bf2.Files[0]}
	return []model.BatchAndFiles{*bf1, *bf2}
}

func getCompletedBatchAndFiles() []model.BatchAndFiles {
	t := time.Now()
	bf1 := new(model.BatchAndFiles)
	_ = faker.FakeData(&bf1)
	bf1.Batch.CompleteTime = &t
	bf1.Batch.SubmitTime = t
	bf1.Batch.Status = "COMPLETED"
	bf1.Files = []model.BatchFile{bf1.Files[0]}
	for idx, _ := range bf1.Files {
		bf1.Files[idx].ResourceType = "OperationOutcome"
	}
	bf2 := new(model.BatchAndFiles)
	_ = faker.FakeData(&bf2)
	bf2.Batch.CompleteTime = &t
	bf2.Batch.SubmitTime = t
	bf2.Batch.Status = "COMPLETED"
	bf2.Files = []model.BatchFile{bf2.Files[0]}
	return []model.BatchAndFiles{*bf1, *bf2}
}
