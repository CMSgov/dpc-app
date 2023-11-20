package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import gov.cms.dpc.fhir.annotations.FHIR;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HAPIExceptionHandlerTest {

    @Test
    void testToResponse_fhirException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        final HAPIExceptionHandler handler = new HAPIExceptionHandler(info);

        String errMsg = "FHIR exception";
        Response response = handler.toResponse(new ServerResponseException(HttpStatus.NOT_FOUND_404, errMsg));
        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());

        OperationOutcome.OperationOutcomeIssueComponent issue = ((OperationOutcome) response.getEntity()).getIssueFirstRep();
        assertEquals(OperationOutcome.IssueType.EXCEPTION, issue.getCode());
        assertEquals(OperationOutcome.IssueSeverity.ERROR, issue.getSeverity());
        assertEquals(errMsg, issue.getDiagnostics());
    }

    @Test
    void testToResponse_nonFhirException() {
        final HAPIExceptionHandler handler = new HAPIExceptionHandler(Mockito.mock(ResourceInfo.class));

        try {
            handler.toResponse(new ServerResponseException(HttpStatus.NOT_FOUND_404, ""));
        } catch (IllegalStateException exception) {
            assertEquals("Cannot return HAPI exception for non-FHIR method", exception.getMessage());
        }
    }

    static class ServerResponseException extends BaseServerResponseException {
        public ServerResponseException(int theStatusCode, String theMessage) {
            super(theStatusCode, theMessage);
        }
    }

    @FHIR
    static class FHIRResourceClass {

    }
}
