package client

import (
	"encoding/json"
	"io/ioutil"
	"path/filepath"
	"strings"
	"time"

	models "github.com/CMSgov/dpc/attribution/model/fhir"

	"github.com/stretchr/testify/mock"
)

// MockBfdClient is for mocking the BFD Client
type MockBfdClient struct {
	mock.Mock
	HICN *string
	MBI  *string
}

// GetExplanationOfBenefit is used for testing
func (bfd *MockBfdClient) GetExplanationOfBenefit(patientID, jobID, cmsID, since string, transactionTime time.Time, serviceDate ClaimsWindow) (*models.Bundle, error) {
	args := bfd.Called(patientID, jobID, cmsID, since, transactionTime, serviceDate)
	if args.Get(0) == nil {
		return nil, args.Error(1)
	}
	return args.Get(0).(*models.Bundle), args.Error(1)
}

// GetPatientByIdentifierHash is used for testing
func (bfd *MockBfdClient) GetPatientByIdentifierHash(hashedIdentifier string) (string, error) {
	args := bfd.Called(hashedIdentifier)
	return args.String(0), args.Error(1)
}

// GetPatient is used for testing
func (bfd *MockBfdClient) GetPatient(patientID, jobID, cmsID, since string, transactionTime time.Time) (*models.Bundle, error) {
	args := bfd.Called(patientID, jobID, cmsID, since, transactionTime)
	return args.Get(0).(*models.Bundle), args.Error(1)
}

// GetCoverage is used for testing
func (bfd *MockBfdClient) GetCoverage(beneficiaryID, jobID, cmsID, since string, transactionTime time.Time) (*models.Bundle, error) {
	args := bfd.Called(beneficiaryID, jobID, cmsID, since, transactionTime)
	return args.Get(0).(*models.Bundle), args.Error(1)
}

// GetData is used for testing
// Returns copy of a static json file (From Blue Button Sandbox originally) after replacing the patient ID of 20000000000001 with the requested identifier
// This is private in the real function and should remain so, but in the test client it makes maintenance easier to expose it.
func (bfd *MockBfdClient) GetData(endpoint, patientID string) (string, error) {
	var fData []byte
	fData, err := ioutil.ReadFile(filepath.Join("testdata/synthetic_beneficiary_data/", filepath.Clean(endpoint)))
	if err != nil {
		fData, err = ioutil.ReadFile(filepath.Join("../testdata/synthetic_beneficiary_data/", filepath.Clean(endpoint)))
		if err != nil {
			return "", err
		}
	}
	cleanData := strings.Replace(string(fData), "20000000000001", patientID, -1)
	if bfd.MBI != nil {
		// no longer hashed, but this is only a test file with synthetic test data
		cleanData = strings.Replace(cleanData, "-1Q03Z002871", *bfd.MBI, -1)
	}
	return cleanData, err
}

// GetBundleData is used for testing
func (bfd *MockBfdClient) GetBundleData(endpoint, patientID string) (*models.Bundle, error) {
	payload, err := bfd.GetData(endpoint, patientID)
	if err != nil {
		return nil, err
	}

	var b models.Bundle
	err = json.Unmarshal([]byte(payload), &b)
	if err != nil {
		return nil, err
	}

	return &b, err
}
