package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.validation.JerseyViolationException;
import org.eclipse.jetty.http.HttpStatus;
import org.glassfish.jersey.server.model.Invocable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.validation.ConstraintViolation;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Jersey exception handling")
public class JerseyExceptionHandlerTest {

    // TODO: add constraint violations

    @Test
    @DisplayName("Cause FHIR exception 🤮")
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
    @DisplayName("Cause non-FHIR exception 🤮")
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
