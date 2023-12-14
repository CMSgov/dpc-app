package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class DefaultFHIRExceptionHandlerTest {

    @Test
    void testToResponse_fhirException_webApplicationException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        final DefaultFHIRExceptionHandler handler = new DefaultFHIRExceptionHandler(info);

        String errMsg = "FHIR exception";
        Response response = handler.toResponse(new WebApplicationException(errMsg, HttpStatus.NOT_FOUND_404));
        assertEquals(HttpStatus.NOT_FOUND_404, response.getStatus());

        OperationOutcome.OperationOutcomeIssueComponent issue = ((OperationOutcome) response.getEntity()).getIssueFirstRep();
        assertEquals(OperationOutcome.IssueSeverity.FATAL, issue.getSeverity());
        assertEquals(errMsg, issue.getDetails().getText());
    }

    @Test
    void testToResponse_fhirException_otherException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        final DefaultFHIRExceptionHandler handler = new DefaultFHIRExceptionHandler(info);

        String errMsg = "FHIR exception";
        Response response = handler.toResponse(new Exception(errMsg));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatus());

        OperationOutcome.OperationOutcomeIssueComponent issue = ((OperationOutcome) response.getEntity()).getIssueFirstRep();
        assertEquals(OperationOutcome.IssueSeverity.FATAL, issue.getSeverity());
        assertEquals(errMsg, issue.getDetails().getText());
    }

    @Test
    void testToResponse_nonFhirException() {
        final DefaultFHIRExceptionHandler handler = new DefaultFHIRExceptionHandler(Mockito.mock(ResourceInfo.class));

        Response response = handler.toResponse(new Exception());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @FHIR
    static class FHIRResourceClass {

    }
}
