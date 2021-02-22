package fhirror

import (
	"context"
	"encoding/json"
	"github.com/go-chi/chi/middleware"
	"github.com/kinbiko/jsonassert"
	"github.com/stretchr/testify/assert"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestBusinessViolation(t *testing.T) {
	w := httptest.NewRecorder()
	c := context.WithValue(context.Background(), middleware.RequestIDKey, "testRequest")
	ja := jsonassert.New(t)

	BusinessViolation(w, c, http.StatusOK, "test")

	resp := w.Result()
	body, _ := ioutil.ReadAll(resp.Body)
	assert.True(t, json.Valid(body))
	ja.Assertf(string(body), `
    {
        "issue": [
            {
                "severity": "warning",
                "code": "Business Rule Violation",
                "details": {
                    "text": "test"
                },
                "diagnostics": "testRequest"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)
}

func TestServerIssue(t *testing.T) {
	w := httptest.NewRecorder()
	c := context.WithValue(context.Background(), middleware.RequestIDKey, "testRequest")
	ja := jsonassert.New(t)

	ServerIssue(w, c, http.StatusOK, "test")

	resp := w.Result()
	body, _ := ioutil.ReadAll(resp.Body)
	assert.True(t, json.Valid(body))
	ja.Assertf(string(body), `
    {
        "issue": [
            {
                "severity": "error",
                "code": "Exception",
                "details": {
                    "text": "test"
                },
                "diagnostics": "testRequest"
            }
        ],
        "resourceType": "OperationOutcome"
    }`)
}
