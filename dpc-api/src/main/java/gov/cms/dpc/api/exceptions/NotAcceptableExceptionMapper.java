package gov.cms.dpc.api.exceptions;

import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotAcceptableExceptionMapper implements ExceptionMapper<NotAcceptableException> {
    @Override
    public Response toResponse(NotAcceptableException exception) {
        Response r = Response.status(Response.Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(exception.getMessage()).build();
        return r;
    }
}
