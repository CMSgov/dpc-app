package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(BufferedLoggerHandler.class)
public class GroupResourceTest extends AbstractSecureApplicationTest {

    private final FhirContext ctx = FhirContext.forR4();
    private final IdType rosterId = new IdType("Group", UUID.randomUUID().toString());
    private final Parameters memberParameters = new Parameters()
            .addParameter(new Parameters.ParametersParameterComponent().setResource(new Group()));

    @Test
    void testCreateRoster() {
        final IGenericClient client = buildV3Client();

        ICreateTyped rosterCreate = client.create()
                .resource(new Group())
                .encodedJson();
        assertThrows(NotImplementedOperationException.class, rosterCreate::execute, "Should throw a 501");
    }

    @Test
    void testGetRoster() {
        final IGenericClient client = buildV3Client();

        IReadExecutable<Group> rosterAdd = client.read()
                .resource(Group.class)
                .withId(rosterId)
                .encodedJson();
        assertThrows(NotImplementedOperationException.class, rosterAdd::execute, "Should throw a 501");
    }

    @Test
    void testUpdateRoster() {
        final IGenericClient client = buildV3Client();

        IUpdateExecutable rosterUpdate = client.update()
                .resource(new Group())
                .withId(rosterId)
                .encodedJson();
        assertThrows(NotImplementedOperationException.class, rosterUpdate::execute, "Should throw a 501");
    }

    @Test
    void testAddMemberToRoster() {
        final IGenericClient client = buildV3Client();

        IOperationUntypedWithInput<Group> memberAdd = client
                .operation()
                .onInstance(rosterId)
                .named("$member-add")
                .withParameters(memberParameters)
                .returnResourceType(Group.class)
                .encodedJson();
        assertThrows(NotImplementedOperationException.class, memberAdd::execute, "Should throw a 501");
    }

    @Test
    void testRemoveMemberFromRoster() {
        final IGenericClient client = buildV3Client();

        IOperationUntypedWithInput<Group> memberRemove = client
                .operation()
                .onInstance(rosterId)
                .named("$member-remove")
                .withParameters(memberParameters)
                .returnResourceType(Group.class)
                .encodedJson();
        assertThrows(NotImplementedOperationException.class, memberRemove::execute, "Should throw a 501");
    }

    private IGenericClient buildV3Client() {
        final String v3Endpoint = getBaseURL().replaceFirst("/v1$", "/v3");
        // v3 has no /metadata endpoint to validate against
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient(v3Endpoint);
        client.registerInterceptor(new LoggingInterceptor(false));
        return client;
    }
}
