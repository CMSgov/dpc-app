package middleware

import (
	"context"
	"github.com/CMSgov/dpc/api/apitest"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"testing"
)

type FHIRFilterTestSuite struct {
	suite.Suite
}

func TestFHIRFilterTestSuite(t *testing.T) {
	suite.Run(t, new(FHIRFilterTestSuite))
}

func (suite *FHIRFilterTestSuite) TestFilteringOrganization() {
	b, _ := Filter(context.Background(), []byte(apitest.Orgjson))
	o, _ := fhir.UnmarshalOrganization(b)
	assert.Nil(suite.T(), o.Contact)
	assert.Nil(suite.T(), o.Telecom)
	assert.Nil(suite.T(), o.Active)
	assert.Nil(suite.T(), o.Text)

	assert.NotNil(suite.T(), o.Identifier)
	assert.NotNil(suite.T(), o.Name)
	assert.NotNil(suite.T(), o.Address)
}

func (suite *FHIRFilterTestSuite) TestFilteringPractitioner() {
	b, _ := Filter(context.Background(), []byte(apitest.Practitionerjson))
	p, _ := fhir.UnmarshalPractitioner(b)
	assert.Nil(suite.T(), p.Telecom)
	assert.Nil(suite.T(), p.Address)
	assert.Nil(suite.T(), p.Gender)
	assert.Nil(suite.T(), p.BirthDate)
	assert.Nil(suite.T(), p.Active)
	assert.Nil(suite.T(), p.Text)

	assert.NotNil(suite.T(), p.Identifier)
	assert.NotNil(suite.T(), p.Name)
}

func (suite *FHIRFilterTestSuite) TestFilteringPatient() {
	b, _ := Filter(context.Background(), []byte(apitest.Patientjson))
	p, _ := fhir.UnmarshalPatient(b)
	assert.Nil(suite.T(), p.Telecom)
	assert.Nil(suite.T(), p.Address)
	assert.Nil(suite.T(), p.Communication)
	assert.Nil(suite.T(), p.ManagingOrganization)
	assert.Nil(suite.T(), p.Contact)
	assert.Nil(suite.T(), p.MaritalStatus)
	assert.Nil(suite.T(), p.Telecom)
	assert.Nil(suite.T(), p.Active)
	assert.Nil(suite.T(), p.Text)

	assert.NotNil(suite.T(), p.Gender)
	assert.NotNil(suite.T(), p.BirthDate)
	assert.NotNil(suite.T(), p.Identifier)
	assert.NotNil(suite.T(), p.Name)
}

func (suite *FHIRFilterTestSuite) TestFilteringError() {
	_, err := Filter(context.Background(), apitest.MalformedOrg())
	assert.Error(suite.T(), err)
}
