package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.eclipse.jetty.http.HttpStatus;
import org.hibernate.exception.ConstraintViolationException;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.persistence.PersistenceException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersistenceExceptionHandlerTest {

    @Test
    void testToResponse_fhirException_constraintViolationException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        final PersistenceExceptionHandler handler = new PersistenceExceptionHandler(info);

        String errMsg = "SQL exception";
        ConstraintViolationException exception = new ConstraintViolationException("", new SQLException(errMsg), "constraintName");
        Response response = handler.toResponse(new PersistenceException(exception));
        assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatus());
        OperationOutcome.OperationOutcomeIssueComponent issue = ((OperationOutcome) response.getEntity()).getIssueFirstRep();
        assertEquals(OperationOutcome.IssueSeverity.FATAL, issue.getSeverity());
        assertEquals(errMsg, issue.getDetails().getText());
    }

    @Test
    void testToResponse_fhirException_otherException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        final PersistenceExceptionHandler handler = new PersistenceExceptionHandler(info);

        Response response = handler.toResponse(new PersistenceException(new Exception()));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatus());
        OperationOutcome.OperationOutcomeIssueComponent issue = ((OperationOutcome) response.getEntity()).getIssueFirstRep();
        assertEquals(OperationOutcome.IssueSeverity.FATAL, issue.getSeverity());
        assertEquals("Internal server error", issue.getDetails().getText());
    }

    @Test
    void testToResponse_nonFhirException_constraintViolationException() {
        final PersistenceExceptionHandler handler = new PersistenceExceptionHandler(Mockito.mock(ResourceInfo.class));

        String errMsg = "SQL exception";
        ConstraintViolationException exception = new ConstraintViolationException("", new SQLException(errMsg), "constraintName");
        Response response = handler.toResponse(new PersistenceException(exception));
        assertEquals(HttpStatus.BAD_REQUEST_400, response.getStatus());
        String issue = (String) response.getEntity();
        assertTrue(issue.contains(errMsg));
    }

    @Test
    void testToResponse_nonFhirException_otherException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        final PersistenceExceptionHandler handler = new PersistenceExceptionHandler(info);

        Response response = handler.toResponse(new PersistenceException(new Exception()));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR_500, response.getStatus());
        String issue = (String) response.getEntity();
        assertTrue(issue.contains("Internal server error"));
    }

    @FHIR
    static class FHIRResourceClass {

    }
}
