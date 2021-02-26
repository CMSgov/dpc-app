package fhirror

import (
	"context"
	"fmt"
	"github.com/darahayes/go-boom"
	"github.com/go-chi/chi/middleware"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"net/http"
)

// GenericServerIssue Write a generic 500 server error OperationOutcome to the response
func GenericServerIssue(ctx context.Context, w http.ResponseWriter) {
	fhirError(ctx, w, http.StatusInternalServerError, fhir.IssueSeverityError, fhir.IssueTypeException, "Internal Server Error")
}

// ServerIssue Write a generic Error/Exception OperationOutcome to the response
func ServerIssue(ctx context.Context, w http.ResponseWriter, statusCode int, message string) {
	fhirError(ctx, w, statusCode, fhir.IssueSeverityError, fhir.IssueTypeException, message)
}

// NotFound Write a specific not found OperationOutcome to the response
func NotFound(ctx context.Context, w http.ResponseWriter, message string) {
	fhirError(ctx, w, http.StatusNotFound, fhir.IssueSeverityWarning, fhir.IssueTypeNotFound, message)
}

// BusinessViolation Write a generic business rule OperationOutcome to the response
func BusinessViolation(ctx context.Context, w http.ResponseWriter, statusCode int, message string) {
	fhirError(ctx, w, statusCode, fhir.IssueSeverityWarning, fhir.IssueTypeBusinessRule, message)
}

func fhirError(ctx context.Context, w http.ResponseWriter, statusCode int, severity fhir.IssueSeverity, code fhir.IssueType, message string) {

	rqID := fmt.Sprintf("%s", ctx.Value(middleware.RequestIDKey))
	o := fhir.OperationOutcome{
		Issue: []fhir.OperationOutcomeIssue{
			{
				Severity:    severity,
				Code:        code,
				Diagnostics: &rqID,
				Details: &fhir.CodeableConcept{
					Text: &message,
				},
			},
		},
	}
	b, err := o.MarshalJSON()
	if err != nil {
		boom.Internal(w, message)
	}

	w.WriteHeader(statusCode)
	if _, err := w.Write(b); err != nil {
		boom.Internal(w, err.Error())
	}
}
