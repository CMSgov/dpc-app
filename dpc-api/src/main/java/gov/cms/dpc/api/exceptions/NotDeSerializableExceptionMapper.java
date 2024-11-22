package gov.cms.dpc.api.exceptions;

import com.github.nitram509.jmacaroons.NotDeSerializableException;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotDeSerializableExceptionMapper implements ExceptionMapper<NotDeSerializableException> {
    @Override
    public Response toResponse(NotDeSerializableException exception) {
        Response r = Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(exception.getMessage()).build();
        return r;
    }
}
