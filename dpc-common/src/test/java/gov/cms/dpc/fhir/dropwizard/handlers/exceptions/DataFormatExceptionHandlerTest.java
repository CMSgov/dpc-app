package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.parser.DataFormatException;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;

public class DataFormatExceptionHandlerTest {

    @Test
    void testHandleFHIRException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        final DataFormatExceptionHandler handler = new DataFormatExceptionHandler(info);

        String msg =  "FHIR exception";
        try {
            Response response = handler.handleFHIRException(new DataFormatException(msg));
            assert response.getStatus() == HttpStatus.BAD_REQUEST_400;
        } catch (DataFormatException e) {
            assert e.getMessage().equals(msg);
        }
    }

    @Test
    void testHandleNonFHIRException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        final DataFormatExceptionHandler handler = new DataFormatExceptionHandler(info);

        try {
            handler.handleNonFHIRException(new DataFormatException("Non-FHIR exception"));
        } catch (IllegalStateException e) {
            assert e.getMessage().equals("Cannot throw FHIR Parser exception from non-FHIR endpoint");
        }
    }
}
