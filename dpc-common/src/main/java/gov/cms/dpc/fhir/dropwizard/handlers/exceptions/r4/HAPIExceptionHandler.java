package gov.cms.dpc.fhir.dropwizard.handlers.exceptions.r4;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.r4.model.OperationOutcome;


/**
 * All BaseServerResponseExceptions are HAPI FHIR exceptions and should be treated the same.  It's possible to have a
 * non-FHIR end point make a FHIR call that throws a BaseServerResponseException, and in that case we'll still return
 * an OperationOutcome in the response body and the correct HTTP status code.
 */
public class HAPIExceptionHandler extends LoggingExceptionMapper<BaseServerResponseException> {

    @Inject
    public HAPIExceptionHandler() {super();}

    @Override
    public Response toResponse(BaseServerResponseException exception) {
        final long exceptionID = super.logException(exception);

        // If the exception contains a FHIR OperationOutcome, use it.  If not, create one.
        OperationOutcome operationOutcome = exception.getOperationOutcome() instanceof OperationOutcome outcome ? outcome : null;
        if (operationOutcome == null) {
            operationOutcome = new OperationOutcome();
            operationOutcome.addIssue()
                    .setCode(OperationOutcome.IssueType.EXCEPTION)
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setDiagnostics(exception.getMessage());
        }
        operationOutcome.setId(String.format("%016x", exceptionID));

        return Response
                .status(exception.getStatusCode())
                .type(FHIRMediaTypes.FHIR_JSON)
                .entity(operationOutcome)
                .build();
    }
}
