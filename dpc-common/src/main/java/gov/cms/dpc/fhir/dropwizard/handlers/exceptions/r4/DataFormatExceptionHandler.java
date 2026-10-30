package gov.cms.dpc.fhir.dropwizard.handlers.exceptions.r4;

import gov.cms.dpc.fhir.dropwizard.handlers.exceptions.AbstractFHIRExceptionHandler;
import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OperationOutcome;

public class DataFormatExceptionHandler extends AbstractFHIRExceptionHandler<DataFormatException> {

    @Inject
    public DataFormatExceptionHandler(@Context ResourceInfo info) {
        super(info);
    }

    @Override
    public Response toResponse(DataFormatException exception) {
        return isFHIRResource() ? handleFHIRException(exception) : handleNonFHIRException(exception);
    }

    @Override
    protected Response handleFHIRException(DataFormatException exception) {
        final long exceptionID = super.logException(exception);

        final OperationOutcome outcome = new OperationOutcome();
        outcome.setId(exceptionIDtoHex(exceptionID));
        outcome
                .addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.FATAL)
                .setDetails(new CodeableConcept().setText(exception.getLocalizedMessage()));

        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(outcome)
                .type(FHIRMediaTypes.FHIR_JSON)
                .build();
    }

    @Override
    protected Response handleNonFHIRException(DataFormatException exception) {
        throw new IllegalStateException("Cannot throw FHIR Parser exception from non-FHIR endpoint");
    }
}
