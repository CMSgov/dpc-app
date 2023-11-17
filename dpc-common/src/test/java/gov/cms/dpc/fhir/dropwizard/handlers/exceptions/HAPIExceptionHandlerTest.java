package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import gov.cms.dpc.fhir.annotations.FHIR;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

public class HAPIExceptionHandlerTest {

    @Test
    void testToResponse_fhirException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        final HAPIExceptionHandler handler = new HAPIExceptionHandler(info);

        String msg = "FHIR exception";
        try {
            Response response = handler.toResponse(new ServerResponseException(HttpStatus.NOT_FOUND_404, msg));
            assert response.getStatus() == HttpStatus.NOT_FOUND_404;
        } catch (BaseServerResponseException e) {
            assert e.getMessage().equals(msg);
        }
    }

    @Test
    void testToResponse_nonFhirException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        final HAPIExceptionHandler handler = new HAPIExceptionHandler(info);

        try {
            Response response = handler.toResponse(new ServerResponseException(HttpStatus.NOT_FOUND_404, "Non-FHIR exception"));
            assert response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR_500;
        } catch (IllegalStateException e) {
            assert e.getMessage().equals("Cannot return HAPI exception for non-FHIR method");
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
