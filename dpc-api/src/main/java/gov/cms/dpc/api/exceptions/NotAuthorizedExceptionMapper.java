package gov.cms.dpc.api.exceptions;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.MediaType;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotAuthorizedExceptionMapper implements ExceptionMapper<NotAuthorizedException> {
    @Override
    public Response toResponse(NotAuthorizedException exception) {
        Response r = Response.status(Response.Status.UNAUTHORIZED).type(MediaType.APPLICATION_JSON).entity(exception.getMessage()).build();
        return r;
    }
}
