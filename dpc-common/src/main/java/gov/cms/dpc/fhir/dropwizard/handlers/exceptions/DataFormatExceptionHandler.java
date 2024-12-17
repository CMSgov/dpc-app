package gov.cms.dpc.fhir.dropwizard.handlers.exceptions;

import ca.uhn.fhir.parser.DataFormatException;
import com.google.inject.Inject;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;

@Provider
public class DataFormatExceptionHandler extends AbstractFHIRExceptionHandler<DataFormatException> {

    @Inject
    DataFormatExceptionHandler(@Context ResourceInfo info) {
        super(info);
    }

    @Override
    Response handleFHIRException(DataFormatException exception) {
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
    Response handleNonFHIRException(DataFormatException exception) {
        throw new IllegalStateException("Cannot throw FHIR Parser exception from non-FHIR endpoint");
    }
}
