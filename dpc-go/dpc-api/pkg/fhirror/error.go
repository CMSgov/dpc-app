package fhirror

import (
	"github.com/darahayes/go-boom"
	"github.com/samply/golang-fhir-models/fhir-models/fhir"
	"net/http"
)

func ServerIssue(w http.ResponseWriter, statusCode int, message string) {
	fhirError(w, statusCode, fhir.IssueSeverityError, fhir.IssueTypeException, message)
}

func BusinessViolation(w http.ResponseWriter, statusCode int, message string) {
	fhirError(w, statusCode, fhir.IssueSeverityWarning, fhir.IssueTypeBusinessRule, message)
}

func fhirError(w http.ResponseWriter, statusCode int, severity fhir.IssueSeverity, code fhir.IssueType, message string) {

	o := fhir.OperationOutcome{
		Issue: []fhir.OperationOutcomeIssue{
			{
				Severity: severity,
				Code:     code,
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

	w.Header().Set("Content-Type", "application/json; charset=UTF-8")
	w.WriteHeader(statusCode)
	if _, err := w.Write(b); err != nil {
		boom.Internal(w, err.Error())
	}
}
