package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.validation.JerseyViolationException;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.server.model.Invocable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintViolation;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Response;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JerseyExceptionHandlerTest {

    // TODO: add constraint violations

    @Test
    void testToResponse_fhirException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        final JerseyExceptionHandler handler = new JerseyExceptionHandler(info);

        Set<ConstraintViolation<?>> violations = Set.of();
        Invocable invocable = Mockito.mock(Invocable.class);
        Response response = handler.toResponse(new JerseyViolationException(violations, invocable));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY_422, response.getStatus());
    }

    @Test
    void testToResponse_nonFhirException() {
        final JerseyExceptionHandler handler = new JerseyExceptionHandler(Mockito.mock(ResourceInfo.class));

        Set<ConstraintViolation<?>> violations = Set.of();
        Invocable invocable = Mockito.mock(Invocable.class);
        Response response = handler.toResponse(new JerseyViolationException(violations, invocable));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY_422, response.getStatus());
    }

    @FHIR
    static class FHIRResourceClass {

    }

}
