package gov.cms.dpc.api.exceptions;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {
    @Override
    public Response toResponse(BadRequestException exception) {
        Response r = Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(exception.getMessage()).build();
        return r;
    }
}
