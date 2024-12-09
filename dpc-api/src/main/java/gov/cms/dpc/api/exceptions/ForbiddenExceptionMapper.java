package gov.cms.dpc.api.exceptions;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.MediaType;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
    @Override
    public Response toResponse(ForbiddenException exception) {
        Response r = Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(exception.getMessage()).build();
        return r;
    }
}
