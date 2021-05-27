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

func (suite *ContextTestSuite) TestAuthCtxForbidden() {
	req := httptest.NewRequest(http.MethodPost, "http://www.your-domain.com", nil)
	res := httptest.NewRecorder()

	l := AuthCtx(nil)
	l.ServeHTTP(res, req)

	r := res.Result()

	assert.Equal(suite.T(), http.StatusForbidden, r.StatusCode)
}

func (suite *ContextTestSuite) TestAuthCtxOK() {

	req := httptest.NewRequest(http.MethodPost, "http://www.your-domain.com", nil)
	req.Header.Set(OrgHeader, "12345")
	res := httptest.NewRecorder()

	l := AuthCtx(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		assert.Equal(suite.T(), "12345", r.Context().Value(ContextKeyOrganization))
	}))
	l.ServeHTTP(res, req)

	r := res.Result()
	assert.Equal(suite.T(), http.StatusOK, r.StatusCode)
}
