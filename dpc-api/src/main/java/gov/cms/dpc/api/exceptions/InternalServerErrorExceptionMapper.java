package gov.cms.dpc.api.exceptions;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InternalServerErrorExceptionMapper implements ExceptionMapper<InternalServerErrorException> {
    @Override
    public Response toResponse(InternalServerErrorException exception) {
        Response r = Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(exception.getMessage()).build();
        return r;
    }
}
