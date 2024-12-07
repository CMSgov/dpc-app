package gov.cms.dpc.api.auth;

import io.dropwizard.auth.UnauthorizedHandler;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.OperationOutcome;
import com.google.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;

public class DPCUnauthorizedHandler implements UnauthorizedHandler {

    @Inject
    public DPCUnauthorizedHandler() {
    }

    @Override
    public Response buildResponse(String prefix, String realm) {
        final var outcome = new OperationOutcome();
        final var coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/ValueSet/operation-outcome");
        coding.setCode("HTTP 401 Unauthorized, Credentials are required to access this resource.");
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDetails(new CodeableConcept().addCoding(coding).setText("HTTP 401 Unauthorized, Credentials are required to access this resource."));

        return Response.status(Status.UNAUTHORIZED)
                .type(FHIR_JSON)
                .entity(outcome)
                .build();
    }

}