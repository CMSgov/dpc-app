package middleware

import (
	"fmt"
	"github.com/CMSgov/dpc/api/constants"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

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
		types, _ = r.Context().Value(constants.ContextKeyResourceTypes).(string)
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id/$export?_type=Coverage,ExplanationOfBenefit&_since=2012-01-02T12:12:12-05:00", nil)
	res := httptest.NewRecorder()

	e := ExportTypesParamCtx(nextHandler)
	e.ServeHTTP(res, req)
	assert.Equal(suite.T(), "Coverage,ExplanationOfBenefit", types)
}

func (suite *ContextTestSuite) TestExportTypesParamDefault() {
	var types string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		types, _ = r.Context().Value(constants.ContextKeyResourceTypes).(string)
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id/$export?_since=2012-01-02T12:12:12-05:00", nil)
	res := httptest.NewRecorder()

	e := ExportTypesParamCtx(nextHandler)
	e.ServeHTTP(res, req)
	assert.Equal(suite.T(), constants.AllResources, types)
}

func (suite *ContextTestSuite) TestExportTypesParamInvalid() {
	var types string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		types, _ = r.Context().Value(constants.ContextKeyResourceTypes).(string)
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id/$export?_type=foobar", nil)
	res := httptest.NewRecorder()

	e := ExportTypesParamCtx(nextHandler)
	e.ServeHTTP(res, req)
	assert.Equal(suite.T(), "", types)
}

func (suite *ContextTestSuite) TestExportSinceParamMinusTZ() {
	var since string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		since, _ = r.Context().Value(constants.ContextKeySince).(string)
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id/$export?_type=Coverage,ExplanationOfBenefit&_since=2012-01-02T12:12:12-05:00", nil)
	res := httptest.NewRecorder()

	e := ExportSinceParamCtx(nextHandler)
	e.ServeHTTP(res, req)
	assert.Equal(suite.T(), "2012-01-02T12:12:12-05:00", since)
}

func (suite *ContextTestSuite) TestExportSinceParamPlusTZ() {
	var since string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		since, _ = r.Context().Value(constants.ContextKeySince).(string)
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id/$export?_type=Coverage,ExplanationOfBenefit&_since=2012-01-02T12:12:12+05:00", nil)
	res := httptest.NewRecorder()

	e := ExportSinceParamCtx(nextHandler)
	e.ServeHTTP(res, req)
	assert.Equal(suite.T(), "", since)
}

func (suite *ContextTestSuite) TestExportSinceParamPlusTZEncoded() {
	var since string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		since, _ = r.Context().Value(constants.ContextKeySince).(string)
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id/$export?_type=Coverage,ExplanationOfBenefit&_since=2012-01-02T12:12:12%2b05:00", nil)
	res := httptest.NewRecorder()

	e := ExportSinceParamCtx(nextHandler)
	e.ServeHTTP(res, req)
	assert.Equal(suite.T(), "2012-01-02T12:12:12+05:00", since)
}

func (suite *ContextTestSuite) TestExportSinceParamDefault() {
	var since string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		since, _ = r.Context().Value(constants.ContextKeySince).(string)
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id/$export?_type=Coverage,ExplanationOfBenefit", nil)
	res := httptest.NewRecorder()

	e := ExportSinceParamCtx(nextHandler)
	e.ServeHTTP(res, req)
	assert.Equal(suite.T(), "", since)
}

func (suite *ContextTestSuite) TestExportSinceParamFuture() {
	future := time.Now().Add(time.Hour * 3).Format(constants.SinceLayout)
	url := fmt.Sprintf("http://www.example.com/Group/some-id/$export?_type=Coverage,ExplanationOfBenefit&_since=%s", future)
	var since string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		since, _ = r.Context().Value(constants.ContextKeySince).(string)
	})

	req := httptest.NewRequest(http.MethodGet, url, nil)
	res := httptest.NewRecorder()

	e := ExportSinceParamCtx(nextHandler)
	e.ServeHTTP(res, req)
	assert.Equal(suite.T(), "", since)
}

func (suite *ContextTestSuite) TestExportSinceParamInvalid() {
	var since string
	nextHandler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		since, _ = r.Context().Value(constants.ContextKeySince).(string)
	})

	req := httptest.NewRequest(http.MethodGet, "http://www.example.com/Group/some-id/$export?_type=Coverage,ExplanationOfBenefit&_since=foobar", nil)
	res := httptest.NewRecorder()

	e := ExportSinceParamCtx(nextHandler)
	e.ServeHTTP(res, req)
	assert.Equal(suite.T(), "", since)
}
