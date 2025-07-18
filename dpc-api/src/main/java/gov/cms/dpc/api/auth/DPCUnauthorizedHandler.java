package gov.cms.dpc.api.auth;

import io.dropwizard.auth.UnauthorizedHandler;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.OperationOutcome;

import static gov.cms.dpc.fhir.FHIRMediaTypes.FHIR_JSON;

public class DPCUnauthorizedHandler implements UnauthorizedHandler {

    @Inject
    public DPCUnauthorizedHandler() {
        // no-op
    }

    @Override
    public Response buildResponse(String prefix, String realm) {
        final var outcome = new OperationOutcome();
        final var coding = new Coding();
        final var concept = new CodeableConcept();
        coding.setSystem("http://hl7.org/fhir/operation-outcome");
        coding.setCode("MSG_AUTH_REQUIRED");
        concept.addCoding(coding);
        concept.setText("HTTP 401 Unauthorized, Credentials are required to access this resource.");
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.EXCEPTION)
                .setDetails(concept);

        return Response.status(Status.UNAUTHORIZED)
                .type(FHIR_JSON)
                .entity(outcome)
                .build();
    }

}
