package middleware

import (
	"context"
	"testing"

	"github.com/CMSgov/dpc/api/apitest"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
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
	assert.Nil(suite.T(), o.Address)

	assert.NotNil(suite.T(), o.Identifier)
	assert.NotNil(suite.T(), o.Name)
}

func (suite *FHIRFilterTestSuite) TestFilteringGroup() {
	b, _ := Filter(context.Background(), []byte(apitest.Groupjson))
	p, _ := fhir.UnmarshalGroup(b)
	assert.Nil(suite.T(), p.Meta)
	assert.Nil(suite.T(), p.Text)
	assert.Nil(suite.T(), p.Extension)
	assert.Nil(suite.T(), p.Identifier)
	assert.Nil(suite.T(), p.Active)
	assert.NotNil(suite.T(), p.ManagingEntity)

	assert.NotNil(suite.T(), p.Member)
	assert.NotNil(suite.T(), p.Name)
	for _, m := range p.Member {
		assert.NotNil(suite.T(), m.Extension)
		assert.Len(suite.T(), m.Extension, 1)
		assert.NotNil(suite.T(), m.Entity)
		assert.Nil(suite.T(), m.Period)
		assert.Nil(suite.T(), m.Inactive)
		assert.Nil(suite.T(), m.ModifierExtension)
		assert.Nil(suite.T(), m.Id)
	}
}

func (suite *FHIRFilterTestSuite) TestFilteringError() {
	_, err := Filter(context.Background(), apitest.MalformedOrg())
	assert.Error(suite.T(), err)
}
