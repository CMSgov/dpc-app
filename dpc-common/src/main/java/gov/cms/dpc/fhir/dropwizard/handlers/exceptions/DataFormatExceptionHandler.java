package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class DataFormatExceptionHandler extends AbstractFHIRExceptionHandler<DataFormatException> {

    @Inject
    DataFormatExceptionHandler() {
        super();
    }

    @Override
    Response handleFHIRException(DataFormatException exception) {
        final long exceptionID = super.logException(exception);

        final OperationOutcome outcome = new OperationOutcome();
        outcome.setId(Long.toString(exceptionID));
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
    Response handleNonFHIRException(DataFormatException exception) {
        throw new IllegalStateException("Cannot throw FHIR Parser exception from non-FHIR endpoint");
    }
}
