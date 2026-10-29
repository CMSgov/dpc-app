package gov.cms.dpc.attribution.resources.v3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.testing.IntegrationTest;
import gov.cms.dpc.testing.utils.MBIUtil;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@IntegrationTest
public class GroupResourceTest extends AbstractAttributionTest {

    private final FhirContext r4Ctx = FhirContext.forR4();
    private IGenericClient client;

    @BeforeEach
    public void beforeEach() {
        client = AttributionTestHelpers.createFHIRClient(r4Ctx, getServerURL());
    }

    @Override
    protected String getServerURL() {
        return String.format("http://localhost:%s/v3", APPLICATION.getLocalPort());
    }

    @Test
    public void testCreateRoster() {
        assertNotImplemented(() -> client.create()
                .resource(new Group())
                .encodedJson()
                .execute());
    }

    @Test
    public void testReplaceRoster() {
        assertNotImplemented(() -> client.update()
                .resource(new Group())
                .withId(rosterId())
                .encodedJson()
                .execute());
    }

    @Test
    public void testDeleteRoster() {
        assertNotImplemented(() -> client.delete()
                .resourceById(rosterId())
                .encodedJson()
                .execute());
    }

    @Test
    public void testAddMemberToRoster() {
        assertNotImplemented(() -> client
                .operation()
                .onInstance(rosterId())
                .named("$member-add")
                .withParameters(memberParameters())
                .returnResourceType(Group.class)
                .encodedJson()
                .execute());
    }

    @Test
    public void testRemoveMemberFromRoster() {
        assertNotImplemented(() -> client
                .operation()
                .onInstance(rosterId())
                .named("$member-remove")
                .withParameters(memberParameters())
                .returnResourceType(Group.class)
                .encodedJson()
                .execute());
    }

    private static void assertNotImplemented(Executable call) {
        assertThrows(NotImplementedOperationException.class, call);
    }

    private static IdType rosterId() {
        return new IdType("Group", UUID.randomUUID().toString());
    }

    private static Parameters memberParameters() {
        final Parameters parameters = new Parameters();
        parameters.addParameter().setName("memberId").setValue(new StringType(MBIUtil.generateMBI()));
        parameters.addParameter().setName("providerNpi").setValue(new StringType(NPIUtil.generateNPI()));
        return parameters;
    }
}
