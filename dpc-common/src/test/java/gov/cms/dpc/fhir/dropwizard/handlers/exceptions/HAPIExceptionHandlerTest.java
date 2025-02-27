package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HAPIExceptionHandlerTest {

    @Test
    void testToResponse_noOperationOutcome() {
        final HAPIExceptionHandler handler = new HAPIExceptionHandler();

        String errMsg = "FHIR exception";
        Response response = handler.toResponse(new ServerResponseException(HttpStatus.NOT_FOUND_404, errMsg));
        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());

        OperationOutcome.OperationOutcomeIssueComponent issue = ((OperationOutcome) response.getEntity()).getIssueFirstRep();
        assertEquals(OperationOutcome.IssueType.EXCEPTION, issue.getCode());
        assertEquals(OperationOutcome.IssueSeverity.ERROR, issue.getSeverity());
        assertEquals(errMsg, issue.getDiagnostics());
    }

    @Test
    void testToResponse_hasOperationOutcome() {
        final HAPIExceptionHandler handler = new HAPIExceptionHandler();

        String errMsg = "FHIR exception";

        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setDiagnostics(errMsg);

        Response response = handler.toResponse(new ServerResponseException(HttpStatus.NOT_FOUND_404, errMsg, outcome));
        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());

        OperationOutcome.OperationOutcomeIssueComponent issue = ((OperationOutcome) response.getEntity()).getIssueFirstRep();
        assertEquals(OperationOutcome.IssueType.EXCEPTION, issue.getCode());
        assertEquals(OperationOutcome.IssueSeverity.ERROR, issue.getSeverity());
        assertEquals(errMsg, issue.getDiagnostics());
    }

    static class ServerResponseException extends BaseServerResponseException {
        OperationOutcome outcome;

        public ServerResponseException(int theStatusCode, String theMessage) {
            super(theStatusCode, theMessage);
        }
        public ServerResponseException(int theStatusCode, String theMessage, OperationOutcome outcome) {
            super(theStatusCode, theMessage);
            this.outcome = outcome;
        }
    }
}
