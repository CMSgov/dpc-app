package gov.cms.dpc.fhir.helpers;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FHIRHelpersUnitTest {

    static final UUID orgID = UUID.randomUUID();
    static final String orgNPI = NPIUtil.generateNPI();
    static final String MACAROON = "macaroon";
    static final String ADMIN_URL = "admin.url";

    @Test
    void testRegisterOrganization() throws IOException {
        IParser parser = mock(IParser.class);
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(new Organization());
        when(parser.parseResource(any(InputStream.class))).thenReturn(bundle);

        IGenericClient client = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
        IOperationUntypedWithInput<Organization> submitOp = mock(IOperationUntypedWithInput.class);
        when(client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(any(Parameters.class))
                .returnResourceType(Organization.class)
        ).thenReturn(submitOp);
        when(submitOp.encodedJson()).thenReturn(submitOp);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        MockedStatic<HttpClients> mockedHttpClients = mockStatic(HttpClients.class);
        mockedHttpClients.when(HttpClients::createDefault).thenReturn(httpClient);
        CloseableHttpResponse httpResponse = mock(CloseableHttpResponse.class, RETURNS_DEEP_STUBS);
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(httpResponse);
        HttpEntity httpEntity = mock(HttpEntity.class);
        InputStream inputStream = IOUtils.toInputStream(MACAROON, "UTF-8");
        when(httpEntity.getContent()).thenReturn(inputStream);
        when(httpResponse.getEntity()).thenReturn(httpEntity);

        String orgIDStr = orgID.toString();
        String response = assertDoesNotThrow(() ->
                FHIRHelpers.registerOrganization(client, parser, orgIDStr, orgNPI, ADMIN_URL));
        assertEquals(MACAROON, response);

        // Token generation error
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
        when(httpResponse.getStatusLine().getReasonPhrase()).thenReturn("Bad request");
        IllegalStateException stateException = assertThrows(IllegalStateException.class, () ->
                FHIRHelpers.registerOrganization(client, parser, orgIDStr, orgNPI, ADMIN_URL));
        assertEquals("Unable to generate token: " + httpResponse.getStatusLine().getReasonPhrase(), stateException.getMessage());
    }

    @Test
    void testHandleMethodOutcome_nullResource() {
        MethodOutcome outcome = new MethodOutcome();
        WebApplicationException exception = assertThrows(WebApplicationException.class, () ->
                FHIRHelpers.handleMethodOutcome(outcome));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getResponse().getStatus());
        assertEquals("Unable to get resource.", exception.getMessage());
    }

    @Test
    void testHandleMethodOutcome_created() {
        MethodOutcome outcome = new MethodOutcome();
        Organization organization = buildOrganization();
        outcome.setResource(organization);
        outcome.setCreated(true);

        Response response = FHIRHelpers.handleMethodOutcome(outcome);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals(organization, response.getEntity());
    }

    @Test
    void testHandleMethodOutcome_ok() {
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
