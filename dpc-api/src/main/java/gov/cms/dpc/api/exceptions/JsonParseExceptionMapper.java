package gov.cms.dpc.api.exceptions;

import com.fasterxml.jackson.core.JsonParseException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {
    @Override
    public Response toResponse(JsonParseException exception) {
        return Response.status(Response.Status.BAD_REQUEST).entity("Value could not be parsed as JSON").build();
    }
}
