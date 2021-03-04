package conf

import (
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
	"os"
	"testing"
)

type ConfigTestSuite struct {
	suite.Suite
}

func TestConfigTestSuite(t *testing.T) {
	suite.Run(t, new(ConfigTestSuite))
}

func (suite *ConfigTestSuite) TearDownTest() {
	_ = os.Unsetenv("ENV")
}

func (suite *ConfigTestSuite) TestGetAsString() {
	NewConfig("../../configs")
	p := GetAsString("port")
	assert.Equal(suite.T(), "3001", p)
}

func (suite *ConfigTestSuite) TestGetAsStringWithDefault() {
	NewConfig("../../configs")
	p := GetAsString("ports", "5000")
	assert.Equal(suite.T(), "5000", p)
}

func (suite *ConfigTestSuite) TestGetAsStringWithLocalOverride() {
	_ = os.Setenv("ENV", "test")
	NewConfig("../../configs", "./")
	p := GetAsString("port")
	assert.Equal(suite.T(), "6002", p)
}

func (suite *ConfigTestSuite) TestGet() {
	NewConfig("../../configs")
	l := Get("log")
	var t map[string]interface{}
	assert.IsType(suite.T(), t, l)
}
