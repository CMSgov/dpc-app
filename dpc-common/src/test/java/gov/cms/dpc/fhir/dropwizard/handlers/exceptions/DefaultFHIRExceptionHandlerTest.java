package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.annotations.FHIR;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;


public class DefaultFHIRExceptionHandlerTest {

    @Test
    void testToResponse_fhirException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        final DefaultFHIRExceptionHandler handler = new DefaultFHIRExceptionHandler(info);

        String msg = "FHIR exception";
        try {
            Response response = handler.toResponse(new WebApplicationException(msg, HttpStatus.NOT_FOUND_404));
            assert response.getStatus() == HttpStatus.NOT_FOUND_404;
        } catch (WebApplicationException e) {
            assert e.getMessage().equals(msg);
        }

        try {
            Response response = handler.toResponse(new Exception(msg));
            assert response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR_500;
        } catch (Exception e) {
            assert e.getMessage().equals(msg);
        }
    }

    @Test
    void testToResponse_nonFhirException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        final DefaultFHIRExceptionHandler handler = new DefaultFHIRExceptionHandler(info);

        String msg = "Non-FHIR exception";
        try {
            Response response = handler.toResponse(new Exception(msg));
            assert response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR_500;
        } catch (Exception e) {
            assert e.getMessage().equals(msg);
        }
    }

    @FHIR
    static class FHIRResourceClass {

    }
}
