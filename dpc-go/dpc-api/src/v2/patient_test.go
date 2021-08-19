package v2

import (
	"context"
	"github.com/CMSgov/dpc/api/conf"
	"github.com/CMSgov/dpc/api/constants"
	"github.com/bxcodec/faker/v3"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/suite"
	"io/ioutil"
	"net/http/httptest"
	"os"
	"testing"
	"time"
)

type PatientControllerTestSuite struct {
	suite.Suite
	pc  *PatientController
	mjc *MockJobClient
}

func (suite *PatientControllerTestSuite) SetupTest() {
	os.Setenv("DPC_EXPORTPATH", "../../test-data")
	os.Setenv("DPC_JOBTIMEOUTINSECONDS", "1")
	conf.NewConfig("../../configs")
	mjc := new(MockJobClient)
	suite.mjc = mjc
	suite.pc = NewPatientController(suite.mjc)
}

func TestPatientControllerTestSuite(t *testing.T) {
	suite.Run(t, new(PatientControllerTestSuite))
}

func (suite *PatientControllerTestSuite) TestPatientEverything() {
	suite.mjc.On("Export", mock.Anything, mock.Anything).Return([]byte("job-id"), nil)
	suite.mjc.On("Status", mock.Anything, mock.Anything).Return([]byte("[{\"batch\":{\"totalPatients\":11,\"patientsProcessed\":1,\"patientIndex\":-1,\"status\":\"COMPLETED\",\"transactionTime\":\"2021-06-07T20:55:08.681-05:00\",\"submitTime\":\"2021-08-16T14:49:00.735672-05:00\",\"completeTime\":\"2021-08-16T14:49:05.042966-05:00\",\"requestURL\":\"http://localhost:3000/api/v2/Patient/$everything\"},\"files\":[{\"resourceType\":\"Coverage\",\"batchID\":\"8cbc3c80-41ea-459c-ba93-c4a3c2f31485\",\"sequence\":0,\"fileName\":\"8cbc3c80-41ea-459c-ba93-c4a3c2f31485-0.coverage\",\"count\":4,\"checksum\":\"1aab1274b1a277ac178d45c7c0fa62bdc5056a79ebf4101147f75c890c05f6d4\",\"fileLength\":38367},{\"resourceType\":\"ExplanationOfBenefit\",\"batchID\":\"8cbc3c80-41ea-459c-ba93-c4a3c2f31485\",\"sequence\":0,\"fileName\":\"8cbc3c80-41ea-459c-ba93-c4a3c2f31485-0.explanationofbenefit\",\"count\":43,\"checksum\":\"93b0218fa1c614e286cf1e99b651d4bf4f4cdadc6269e3a6b1b32f4fd59ba1d9\",\"fileLength\":1216313},{\"resourceType\":\"Patient\",\"batchID\":\"8cbc3c80-41ea-459c-ba93-c4a3c2f31485\",\"sequence\":0,\"fileName\":\"8cbc3c80-41ea-459c-ba93-c4a3c2f31485-0.patient\",\"count\":1,\"checksum\":\"eae9ba4bac4b90a38630360d5055945f0c128fef7464bd1a5120ca358aff6d4f\",\"fileLength\":3480}]}]"), nil)

	req := httptest.NewRequest("Post", "http://localhost/doesnotmatter", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, constants.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, constants.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, constants.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, constants.ContextKeyResourceTypes, constants.AllResources)
	ctx = context.WithValue(ctx, constants.ContextKeyMBI, "mbi")
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()
	suite.pc.Export(w, req)
	res := w.Result()

	b, _ := ioutil.ReadAll(res.Body)
	bundle, _ := fhir.UnmarshalBundle(b)
	assert.True(suite.T(), len(bundle.Entry) > 1)
	assert.Equal(suite.T(), fhir.BundleTypeSearchset, bundle.Type)
}

func (suite *PatientControllerTestSuite) TestPatientEverythingTimedOut() {
	suite.mjc.On("Export", mock.Anything, mock.Anything).Return([]byte("job-id"), nil)
	suite.mjc.On("Status", mock.Anything, mock.Anything).Return([]byte("hello"), nil).WaitUntil(time.After(time.Second * 3))

	req := httptest.NewRequest("Post", "http://localhost/doesnotmatter", nil)
	ctx := req.Context()
	ctx = context.WithValue(ctx, constants.ContextKeyOrganization, "12345")
	ctx = context.WithValue(ctx, constants.ContextKeyRequestURL, faker.URL())
	ctx = context.WithValue(ctx, constants.ContextKeyRequestingIP, faker.IPv4())
	ctx = context.WithValue(ctx, constants.ContextKeyResourceTypes, constants.AllResources)
	ctx = context.WithValue(ctx, constants.ContextKeyMBI, "mbi")
	req = req.WithContext(ctx)
	w := httptest.NewRecorder()
	suite.pc.Export(w, req)
	res := w.Result()

	b, _ := ioutil.ReadAll(res.Body)
	oo, _ := fhir.UnmarshalOperationOutcome(b)
	assert.NotNil(suite.T(), oo)
}
