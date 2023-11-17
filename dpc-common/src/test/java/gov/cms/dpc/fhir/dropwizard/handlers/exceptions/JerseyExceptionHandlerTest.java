package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import gov.cms.dpc.fhir.annotations.FHIR;
import io.dropwizard.jersey.validation.JerseyViolationException;
import org.glassfish.jersey.server.model.Invocable;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintViolation;
import javax.ws.rs.container.ResourceInfo;
import java.util.Set;

public class JerseyExceptionHandlerTest {

    @Test
    void testToResponse_fhirException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        Mockito.when(info.getResourceClass()).thenAnswer(answer -> FHIRResourceClass.class);
        final JerseyExceptionHandler handler = new JerseyExceptionHandler(info);

        try {
            Set<ConstraintViolation<?>> violations = Set.of();
            Invocable invocable = Mockito.mock(Invocable.class);
            handler.toResponse(new JerseyViolationException(violations, invocable));
        } catch (JerseyViolationException e) {
            assert e.getMessage().isEmpty();
        }
    }

    @Test
    void testToResponse_nonFhirException() {
        ResourceInfo info = Mockito.mock(ResourceInfo.class);
        final JerseyExceptionHandler handler = new JerseyExceptionHandler(info);

        try {
            Set<ConstraintViolation<?>> violations = Set.of();
            Invocable invocable = Mockito.mock(Invocable.class);
            handler.toResponse(new JerseyViolationException(violations, invocable));
        } catch (JerseyViolationException e) {
            assert e.getMessage().isEmpty();
        }
    }

    @FHIR
    static class FHIRResourceClass {

    }

}
