package gov.cms.dpc.fhir.dropwizard.handlers;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;


@Provider
public class HAPIExceptionHandler extends AbstractFHIRExceptionHandler<BaseServerResponseException> {

    @Inject
    HAPIExceptionHandler() {
        super();
    }

    @Override
    Response handleFHIRException(BaseServerResponseException exception) {
        final Response response = super.toResponse(exception);
        final OperationOutcome operationOutcome = (OperationOutcome) exception.getOperationOutcome();

        return Response.fromResponse(response)
                .status(exception.getStatusCode())
                .type(FHIRMediaTypes.FHIR_JSON)
                .entity(operationOutcome)
                .build();
    }

    @Override
    Response handleNonFHIRException(BaseServerResponseException exception) {
        throw new IllegalStateException("Cannot return HAPI exception for non-FHIR method");
    }
}
