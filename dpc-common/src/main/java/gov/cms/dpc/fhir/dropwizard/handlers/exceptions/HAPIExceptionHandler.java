package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.inject.Inject;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class HAPIExceptionHandler extends AbstractFHIRExceptionHandler<BaseServerResponseException> {

    @Inject
    HAPIExceptionHandler(@Context ResourceInfo info) {
        super(info);
    }

    @Override
    public Response toResponse(BaseServerResponseException exception) {
        if (isFHIRResource()) {
            return handleFHIRException(exception);
        } else {
            return handleNonFHIRException(exception);
        }
    }

    @Override
    Response handleFHIRException(BaseServerResponseException exception) {
        final long exceptionID = super.logException(exception);
        OperationOutcome operationOutcome = (OperationOutcome) exception.getOperationOutcome();
        if (operationOutcome == null) {
            operationOutcome = new OperationOutcome();
            operationOutcome.addIssue()
                    .setCode(OperationOutcome.IssueType.EXCEPTION)
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setDiagnostics(exception.getMessage());
        }
        operationOutcome.setId(exceptionIDtoHex(exceptionID));

        return Response
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
