package gov.cms.dpc.api.exceptions;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import javax.ws.rs.core.MediaType;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.eclipse.jetty.http.HttpStatus;

@Provider
public class UnprocessableEntityExceptionMapper implements ExceptionMapper<UnprocessableEntityException> {
    @Override
    public Response toResponse(UnprocessableEntityException exception) {
        Response r = Response.status(HttpStatus.UNPROCESSABLE_ENTITY_422).type(MediaType.APPLICATION_JSON).entity(exception.getMessage()).build();
        return r;
    }
}
