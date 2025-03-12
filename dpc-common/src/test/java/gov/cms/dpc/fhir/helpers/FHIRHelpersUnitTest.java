package gov.cms.dpc.fhir.helpers;

import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FHIRHelpersUnitTest {

    static final UUID orgID = UUID.randomUUID();
    static final String orgNPI = NPIUtil.generateNPI();

    @Test
    void handleMethodOutcome_nullResource() {
        WebApplicationException exception = assertThrows(WebApplicationException.class, () ->
                FHIRHelpers.handleMethodOutcome(new MethodOutcome()));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getResponse().getStatus());
        assertEquals("Unable to get resource.", exception.getMessage());
    }

    @Test
    void handleMethodOutcome_created() {
        MethodOutcome outcome = new MethodOutcome();
        Organization organization = buildOrganization();
        outcome.setResource(organization);
        outcome.setCreated(true);

        Response response = FHIRHelpers.handleMethodOutcome(outcome);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(organization, response.getEntity());
    }

    @Test
    void handleMethodOutcome_ok() {
        MethodOutcome outcome = new MethodOutcome();
        Organization organization = buildOrganization();
        outcome.setResource(organization);

        Response response = FHIRHelpers.handleMethodOutcome(outcome);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(organization, response.getEntity());
    }

    private Organization buildOrganization() {
        Organization organization = new Organization();
        organization.setId(orgID.toString());
        Identifier identifier = new Identifier();
        identifier.setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(orgNPI);
        organization.setIdentifier(List.of(identifier));
        return organization;
    }
}
