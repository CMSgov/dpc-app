package util

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"testing"
)

type FhirTestSuite struct {
	suite.Suite
}

func (suite *FhirTestSuite) SetupTest() {
}

func TestFhirTestSuite(t *testing.T) {
	suite.Run(t, new(FhirTestSuite))
}

func (suite *FhirTestSuite) TestIdentityParsing() {

	idjson := `{
        "identifier": [
        {
            "system": "http://hl7.org/fhir/sid/us-npi",
            "value": "2111111119"
        },
        {
            "system": "https://github.com/synthetichealth/synthea",
            "value": "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0"
        }
        ]
    }`

	npi, err := GetNPI([]byte(idjson))
	assert.Nil(suite.T(), err)
	assert.Equal(suite.T(), "2111111119", npi)

	id, err := GetIdentifier([]byte(idjson), "https://github.com/synthetichealth/synthea")
	assert.Nil(suite.T(), err)
	assert.Equal(suite.T(), "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0", id)

	_, err = GetNPI([]byte("{}"))
	assert.Error(suite.T(), err)

	_, err = GetIdentifier([]byte(idjson), "some-system")
	assert.Error(suite.T(), err)
}
