package dpc

import (
	"testing"
)

func TestTemplateGenerator(t *testing.T) {
	actualResult := string(templateBodyGenerator("../../templates/template-generator-test.json", map[string]func() string{"{me}": func() string { return "hello" }})())
	expectedResult := `{"replace": "hellohellohello{ne}"}`

	if actualResult != expectedResult {
		t.Fatalf("Expected %s but got %s", expectedResult, actualResult)
	}

	actualResult = string(templateBodyGenerator("../../templates/template-generator-test.json", map[string]func() string{"{me}": func() string { return "hello" }, "{ne}": func() string { return "bye" }})())
	expectedResult = `{"replace": "hellohellohellobye"}`

	if actualResult != expectedResult {
		t.Fatalf("Expected %s but got %s", expectedResult, actualResult)
	}
}
