package dpc

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestTemplateGenerator(t *testing.T) {
	actualResult := string(templateBodyGenerator("../../templates/template-generator-test.json", map[string]func() string{"{me}": func() string { return "hello" }})())
	expectedResult := `{"replace": "hellohellohello{ne}"}`
	assert.Equal(t, actualResult, expectedResult, "they should be equal")

	actualResult = string(templateBodyGenerator("../../templates/template-generator-test.json", map[string]func() string{"{me}": func() string { return "hello" }, "{ne}": func() string { return "bye" }})())
	expectedResult = `{"replace": "hellohellohellobye"}`
	assert.Equal(t, actualResult, expectedResult, "they should be equal")

}
