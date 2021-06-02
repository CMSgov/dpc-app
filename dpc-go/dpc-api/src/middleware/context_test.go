package middleware

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/suite"
)

type ContextTestSuite struct {
	suite.Suite
}

func TestContextTestSuite(t *testing.T) {
	suite.Run(t, new(ContextTestSuite))
}

func (suite *ContextTestSuite) TestExportTypesParam() {
	var types string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		types, _ = r.Context().Value(ContextKeyResourceTypes).(string)
		assert.Equal(suite.T(), "Coverage,ExplanationOfBenefit", types)
		_, _ = w.Write([]byte(""))
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id$export?_type=Coverage,ExplanationOfBenefit&_since=2012-01-02T12:12:12-00:00", nil)
	res := httptest.NewRecorder()

	e := ExportTypesParamCtx(nextHandler)
	e.ServeHTTP(res, req)
}

func (suite *ContextTestSuite) TestExportTypesParamDefault() {
	var types string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		types, _ = r.Context().Value(ContextKeyResourceTypes).(string)
		assert.Equal(suite.T(), AllResources, types)
		_, _ = w.Write([]byte(""))
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id$export?_since=2012-01-02T12:12:12-00:00", nil)
	res := httptest.NewRecorder()

	e := ExportTypesParamCtx(nextHandler)
	e.ServeHTTP(res, req)
}

func (suite *ContextTestSuite) TestExportSinceParam() {
	var since string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		since, _ = r.Context().Value(ContextKeySince).(string)
		assert.Equal(suite.T(), "2012-01-02T12:12:12-00:00", since)
		_, _ = w.Write([]byte(""))
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id$export?_type=Coverage,ExplanationOfBenefit&_since=2012-01-02T12:12:12-00:00", nil)
	res := httptest.NewRecorder()

	e := ExportSinceParamCtx(nextHandler)
	e.ServeHTTP(res, req)
}

func (suite *ContextTestSuite) TestExportSinceParamDefault() {
	var since string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		since, _ = r.Context().Value(ContextKeySince).(string)
		assert.Equal(suite.T(), "", since)
		_, _ = w.Write([]byte(""))
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id$export?_type=Coverage,ExplanationOfBenefit", nil)
	res := httptest.NewRecorder()

	e := ExportSinceParamCtx(nextHandler)
	e.ServeHTTP(res, req)
}
