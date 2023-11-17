package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.eclipse.jetty.http.HttpStatus;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.persistence.PersistenceException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.sql.SQLException;

public class PersistenceExceptionHandlerTest {

    @Test
    void testToResponse_nonFhirException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        final PersistenceExceptionHandler handler = new PersistenceExceptionHandler(info);

        String msg = "Constraint violation exception";
        try {
            ConstraintViolationException exception = new ConstraintViolationException(msg, new SQLException("SQL exception"), "constraintName");
            Response response = handler.toResponse(new PersistenceException("Non-FHIR exception", exception));
            assert response.getStatus() == HttpStatus.BAD_REQUEST_400;
        } catch (PersistenceException e) {
            assert e.getMessage().equals(msg);
        }

        try {
            Response response = handler.toResponse(new PersistenceException("Non-FHIR exception", new Exception("Exception")));
            assert response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR_500;
        } catch (PersistenceException e) {
            assert e.getMessage().equals("Internal server error");
        }
    }

    @Test
    void testToResponse_fhirException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        final PersistenceExceptionHandler handler = new PersistenceExceptionHandler(info);

        String msg = "Constraint violation exception";
        try {
            ConstraintViolationException exception = new ConstraintViolationException(msg, new SQLException("SQL exception"), "constraintName");
            Response response = handler.toResponse(new PersistenceException("FHIR exception", exception));
            assert response.getStatus() == HttpStatus.BAD_REQUEST_400;
        } catch (PersistenceException e) {
            assert e.getMessage().equals(msg);
        }

        try {
            Response response = handler.toResponse(new PersistenceException("FHIR exception", new Exception("Exception")));
            assert response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR_500;
        } catch (PersistenceException e) {
            assert e.getMessage().equals("Internal server error");
        }
    }

    @FHIR
    static class FHIRResourceClass {

    }
}
