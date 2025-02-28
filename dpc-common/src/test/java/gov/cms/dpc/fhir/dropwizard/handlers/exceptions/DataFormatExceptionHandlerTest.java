package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.parser.DataFormatException;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class DataFormatExceptionHandlerTest {

    @Test
    void testHandleFHIRException() {
        final DataFormatExceptionHandler handler = new DataFormatExceptionHandler(Mockito.mock(ResourceInfo.class));

        String errMsg = "FHIR exception";
        Response response = handler.handleFHIRException(new DataFormatException(errMsg));
        assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatus());

        OperationOutcome.OperationOutcomeIssueComponent issue = ((OperationOutcome) response.getEntity()).getIssueFirstRep();
        assertEquals(OperationOutcome.IssueSeverity.FATAL, issue.getSeverity());
        assertEquals(errMsg, issue.getDetails().getText());
    }

    @Test
    void testHandleNonFHIRException() {
        final DataFormatExceptionHandler handler = new DataFormatExceptionHandler(Mockito.mock(ResourceInfo.class));

        Exception exception = assertThrows((IllegalStateException.class), () -> handler.handleNonFHIRException(new DataFormatException()));
        assertEquals("Cannot throw FHIR Parser exception from non-FHIR endpoint", exception.getMessage());
    }
}
