package fhirror

import (
	"context"
	"fmt"
	"github.com/darahayes/go-boom"
	"github.com/go-chi/chi/middleware"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"net/http"
)

func ServerIssue(w http.ResponseWriter, ctx context.Context, statusCode int, message string) {
	fhirError(w, ctx, statusCode, fhir.IssueSeverityError, fhir.IssueTypeException, message)
}

func BusinessViolation(w http.ResponseWriter, ctx context.Context, statusCode int, message string) {
	fhirError(w, ctx, statusCode, fhir.IssueSeverityWarning, fhir.IssueTypeBusinessRule, message)
}

func fhirError(w http.ResponseWriter, ctx context.Context, statusCode int, severity fhir.IssueSeverity, code fhir.IssueType, message string) {

	rqId := fmt.Sprintf("%s", ctx.Value(middleware.RequestIDKey))
	o := fhir.OperationOutcome{
		Issue: []fhir.OperationOutcomeIssue{
			{
				Severity:    severity,
				Code:        code,
				Diagnostics: &rqId,
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

	w.Header().Set("Content-Type", "application/fhir+json; charset=UTF-8")
	w.WriteHeader(statusCode)
	if _, err := w.Write(b); err != nil {
		boom.Internal(w, err.Error())
	}
}
