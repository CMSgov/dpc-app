package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Provenance;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroupResourceUnitTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGenericClient attributionClient;

    GroupResource resource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        resource = new GroupResource(null, attributionClient, null, null);
    }

    @Test
    public void testCreateRoster() {
        UUID orgId = UUID.randomUUID();
        Organization organization = new Organization();
        organization.setId(orgId.toString());
        OrganizationPrincipal organizationPrincipal = new OrganizationPrincipal(organization);

        Provenance provenance = new Provenance();
        Provenance.ProvenanceAgentComponent provenanceAgent = new Provenance.ProvenanceAgentComponent();
        provenanceAgent.setWho(new Reference("Practitioner/12345")).addRole().addCoding().setCode("AGNT");
        provenanceAgent.setOnBehalfOf(new Reference("Organization/23456"));
        provenance.addAgent(provenanceAgent);

        Group group = new Group();

        ICreateTyped createExec = Mockito.mock(ICreateTyped.class);
        Mockito.when(attributionClient.create().resource(group).encodedJson()).thenReturn(createExec);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setResource(group);
        Mockito.when(createExec.execute()).thenReturn(outcome);

        Response response = resource.createRoster(organizationPrincipal, provenance, group);
        Group result = (Group) response.getEntity();

        assertEquals("Organization ID", result.getMeta().getTag(DPCIdentifierSystem.DPC.getSystem(), orgId.toString()).getDisplay());
        assertEquals(group, result);
    }

}
