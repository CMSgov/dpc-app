package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(BufferedLoggerHandler.class)
public class GroupResourceTest extends AbstractSecureApplicationTest {

    private final FhirContext ctx = FhirContext.forR4();
    private final IdType rosterId = new IdType("Group", UUID.randomUUID().toString());
    private final Parameters memberParameters = new Parameters()
            .addParameter(new Parameters.ParametersParameterComponent().setResource(new Group()));

    @Test
    void testCreateRoster() throws IOException, URISyntaxException {
        final IGenericClient client = buildV3Client();

        assertThrows(NotImplementedOperationException.class, () -> client.create()
                .resource(new Group())
                .encodedJson()
                .execute(), "Should throw a 501");
    }

    @Test
    void testGetRoster() throws IOException, URISyntaxException {
        final IGenericClient client = buildV3Client();

        assertThrows(NotImplementedOperationException.class, () -> client.read()
                .resource(Group.class)
                .withId(rosterId)
                .encodedJson()
                .execute(), "Should throw a 501");
    }

    @Test
    void testUpdateRoster() throws IOException, URISyntaxException {
        final IGenericClient client = buildV3Client();

        assertThrows(NotImplementedOperationException.class, () -> client.update()
                .resource(new Group())
                .withId(rosterId)
                .encodedJson()
                .execute(), "Should throw a 501");
    }

    @Test
    void testAddMemberToRoster() throws IOException, URISyntaxException {
        final IGenericClient client = buildV3Client();

        assertThrows(NotImplementedOperationException.class, () -> client
                .operation()
                .onInstance(rosterId)
                .named("$member-add")
                .withParameters(memberParameters)
                .returnResourceType(Group.class)
                .encodedJson()
                .execute(), "Should throw a 501");
    }

    @Test
    void testRemoveMemberFromRoster() throws IOException, URISyntaxException {
        final IGenericClient client = buildV3Client();

        assertThrows(NotImplementedOperationException.class, () -> client
                .operation()
                .onInstance(rosterId)
                .named("$member-remove")
                .withParameters(memberParameters)
                .returnResourceType(Group.class)
                .encodedJson()
                .execute(), "Should throw a 501");
    }

    private IGenericClient buildV3Client() throws IOException, URISyntaxException {
        final String v3Endpoint = getBaseURL().replaceFirst("/v1$", "/v3");
        final APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        // v3 has no /metadata endpoint to validate against
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient(v3Endpoint);
        client.registerInterceptor(new BearerTokenAuthInterceptor(auth.accessToken));
        return client;
    }
}
