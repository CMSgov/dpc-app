package gov.cms.dpc.api.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        // Check if the violation is specifically for a missing or empty JWT
        boolean isJwtMissing = exception.getConstraintViolations().stream()
            .anyMatch(violation -> 
                "jwt".equals(violation.getPropertyPath().toString()) &&
                "Must submit JWT".equals(violation.getMessage())
            );

        if (isJwtMissing) {
            // Return a 401 Unauthorized response for missing/empty JWT
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\": \"Must submit JWT\"}")
                .type(MediaType.APPLICATION_JSON)
                .build();
        }

        // Fallback to a 400 Bad Request for other constraint violations
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("{\"error\": \"Invalid request\"}")
            .type(MediaType.APPLICATION_JSON)
            .build();
    }
}
