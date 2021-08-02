package v2

import (
	"context"
	"encoding/json"
	"fmt"
	"github.com/CMSgov/dpc/api/apitest"
	"github.com/CMSgov/dpc/api/conf"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/model"
	"github.com/go-chi/chi/middleware"
	"github.com/kinbiko/jsonassert"
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

func (ac *MockJobClient) Status(ctx context.Context, jobID string) ([]byte, error) {
	args := ac.Called(ctx, jobID)
	return args.Get(0).([]byte), args.Error(1)
}

func (mc *MockJobClient) Export(ctx context.Context, request model.ExportRequest) ([]byte, error) {
    args := mc.Called(ctx, request)
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
	suite.job = NewJobController(suite.mjc)
}

func TestJobControllerTestSuite(t *testing.T) {
	suite.Run(t, new(JobControllerTestSuite))
}

func (suite *JobControllerTestSuite) TestGetStatus() {

	now := time.Now()
	var batches []model.BatchAndFiles
	_ = json.Unmarshal([]byte(apitest.GetBatchAndFilesJSON), &batches)
	batches[0].Batch.TransactionTime = now
	batches[0].Batch.SubmitTime = now
	batches[0].Batch.CompleteTime = &now

	b, _ := json.Marshal(batches)

	suite.mjc.On("Status", mock.Anything, mock.Anything).Return(b, nil)

	ja := jsonassert.New(suite.T())
	req := httptest.NewRequest(http.MethodGet, "http://example.com/foo", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, middleware.RequestIDKey, "12345")
	ctx = context.WithValue(ctx, middleware2.ContextKeyJobID, "54321")
	req = req.WithContext(ctx)

	w := httptest.NewRecorder()

	suite.job.Status(w, req)

	res := w.Result()

	apiPath := conf.GetAsString("apiPath")

	b, _ = ioutil.ReadAll(res.Body)
	ja.Assertf(string(b), fmt.Sprintf(`{
  "transactionTime": "<<PRESENCE>>",
  "request": "http://pfSbNLv.info/",
  "requiresAccessToken": true,
  "output": [
    {
      "type": "Patient",
      "url": "%s/Data/f9185824-c835-421d-81f9-ec2b1ee609af-0.patient.ndjson",
      "count": 1,
      "extension": [
        {
          "url": "https://dpc.cms.gov/checksum",
          "valueString": "ad09ae2eee0a5111508b072cb8c3eaca49f342df82b7c456bcd04df7612283e77f04f0d55e89a5a235f6636a3a9180169b890f6a3078e200fd9a1ca1574885767ffa30ddf94bc374464cf8c6f1da72c8"
        },
        {
          "url": "https://dpc.cms.gov/file_length",
          "valueDecimal": 1234
        }
      ]
    }
  ],
  "error": [],
  "extension": [
    {
      "url": "https://dpc.cms.gov/submit_time",
      "valueDateTime": "<<PRESENCE>>"
    },
    {
      "url": "https://dpc.cms.gov/complete_time",
      "valueDateTime": "<<PRESENCE>>"
    }
  ]
}`, apiPath))
}
