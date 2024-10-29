package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.NotImplementedOperationException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationIT;
import gov.cms.dpc.api.TestOrganizationContext;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.factories.FHIRPractitionerBuilder;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.sql.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;

@DisplayName("Practitioner resource operations")
class PractitionerResourceIT extends AbstractSecureApplicationIT {

    @Test
    @DisplayName("Get all practitioners for an organization 🥳")
    @Order(1)
    void ensurePractitionersExist() throws IOException, URISyntaxException, GeneralSecurityException {
        final IParser parser = ctx.newJsonParser();
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPractitionerTest(client, parser);

        // Find everything attributed
        final Bundle practitioners = client
                .search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(4, practitioners.getTotal(), "Should have all the providers");
    }

    @Test
    @DisplayName("Get a specific practitioner for an organization 🥳")
    @Order(2)
    void ensurePractitionerExists() throws IOException, URISyntaxException, GeneralSecurityException {
        final IParser parser = ctx.newJsonParser();
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPractitionerTest(client, parser);

        final Bundle specificSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("1232131239"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, specificSearch.getTotal(), "Should have a specific provider");

        // Fetch the provider directly
        final Practitioner foundProvider = (Practitioner) specificSearch.getEntryFirstRep().getResource();

        final IReadExecutable<Practitioner> clientQuery = client
                .read()
                .resource(Practitioner.class)
                .withId(foundProvider.getIdElement())
                .encodedJson();

        final Practitioner queriedProvider = clientQuery
                .execute();

        assertTrue(foundProvider.equalsDeep(queriedProvider), "Search and GET should be identical");

    }

    @Test
    @DisplayName("Delete a practitioner 🥳")
    @Order(3)
    void deletePractitioner() throws IOException, URISyntaxException, GeneralSecurityException {
        final IParser parser = ctx.newJsonParser();
        final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPractitionerTest(client, parser);

        final Bundle specificSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("1232131239"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        // Fetch the provider directly
        final Practitioner foundProvider = (Practitioner) specificSearch.getEntryFirstRep().getResource();

        final IReadExecutable<Practitioner> clientQuery = client
                .read()
                .resource(Practitioner.class)
                .withId(foundProvider.getIdElement())
                .encodedJson();

        // Try to delete the practitioner
        client
                .delete()
                .resourceById(foundProvider.getIdElement())
                .encodedJson()
                .execute();

        // Try again, should be not found
        assertThrows(AuthenticationException.class, clientQuery::execute, "Should not have practitioner");
    }

    @Test
    @DisplayName("Create practitioner with invalid parameters 🤮")
    @Order(4)
    void testCreateInvalidPractitioner() throws IOException, URISyntaxException {
        URL url = new URL(getBaseURL() + "/Practitioner");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.POST);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");

        APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

        conn.setDoOutput(true);
        String reqBody = "{\"test\": \"test\"}";
        conn.getOutputStream().write(reqBody.getBytes());

        assertEquals(HttpStatus.BAD_REQUEST_400, conn.getResponseCode());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
            StringBuilder respBuilder = new StringBuilder();
            String respLine = null;
            while ((respLine = reader.readLine()) != null) {
                respBuilder.append(respLine.trim());
            }
            String resp = respBuilder.toString();
            assertTrue(resp.contains("\"resourceType\":\"OperationOutcome\""));
            assertTrue(resp.contains("Invalid JSON content"));
        }

        conn.disconnect();
    }

    @Test
    @DisplayName("Create practitioner 🥳")
    @Order(5)
    public void testCreatePractitionerReturnsAppropriateHeaders() {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        Practitioner practitioner = APITestHelpers.createPractitionerResource(NPIUtil.generateNPI(), APITestHelpers.ORGANIZATION_ID);

        MethodOutcome methodOutcome = client.create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        String location = methodOutcome.getResponseHeaders().get("location").get(0);
        String date = methodOutcome.getResponseHeaders().get("last-modified").get(0);
        assertNotNull(location);
        assertNotNull(date);

        Practitioner foundPractitioner = client.read()
                .resource(Practitioner.class)
                .withUrl(location)
                .encodedJson()
                .execute();

        assertEquals(practitioner.getIdentifierFirstRep().getValue(), foundPractitioner.getIdentifierFirstRep().getValue());

        client.delete()
                .resource(foundPractitioner)
                .encodedJson()
                .execute();
    }

    @Test
    @DisplayName("Update practitioner - not yet implemented 🤮")
    @Order(6)
    public void testUpdatePractitionerNotImplemented() throws IOException {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        final IParser parser = ctx.newJsonParser();
        APITestHelpers.setupPractitionerTest(client, parser);

        // Grab a practitioner to update
        final Bundle practitioners = client
                .search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        final Practitioner practitioner = (Practitioner) practitioners.getEntryFirstRep().getResource();

        practitioner.setBirthDate(Date.valueOf("1989-01-01"));
        IUpdateExecutable update = client
                .update()
                .resource(practitioner)
                .withId(practitioner.getId());

        assertThrows(NotImplementedOperationException.class, update::execute);
    }

    @Test
    @DisplayName("Get pracitioners with valid authorization 🥳")
    @Order(7)
    public void testPractitionerPathAuthorization() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner practitioner1 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgAContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org A Practitioner", "Last name")
                .build();

        Practitioner practitioner2 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgBContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org B Practitioner", "Last name")
                .build();

        final Practitioner orgAPractitioner = (Practitioner) APITestHelpers.createResource(orgAClient, practitioner1).getResource();
        final Practitioner orgBPractitioner = (Practitioner) APITestHelpers.createResource(orgBClient, practitioner2).getResource();

        //Test GET /Practitioner/{id}
        assertNotNull(APITestHelpers.getResourceById(orgAClient, Practitioner.class, orgAPractitioner.getIdElement().getIdPart()));
        assertNotNull(APITestHelpers.getResourceById(orgBClient, Practitioner.class, orgBPractitioner.getIdElement().getIdPart()));
    }

    @Test
    @DisplayName("Get practitioner with invalid authorization 🤮")
    @Order(8)
    public void testPractitionerPathInvalidAuthorization() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner practitioner1 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgAContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org A Practitioner", "Last name")
                .build();

        final Practitioner orgAPractitioner = (Practitioner) APITestHelpers.createResource(orgAClient, practitioner1).getResource();

        //Test GET /Practitioner/{id}
        assertThrows(AuthenticationException.class,
                () -> APITestHelpers.getResourceById(orgBClient, Practitioner.class, orgAPractitioner.getIdElement().getIdPart())
                , "Expected auth exception when accessing another org's practitioner.");
    }

    @Test
    @DisplayName("Put practitioners with invalid authorization 🤮")
    @Order(9)
    public void testPutPractitionerPathInvalidAuthorization() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner practitioner1 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgAContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org A Practitioner", "Last name")
                .build();

        Practitioner practitioner2 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgBContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org B Practitioner", "Last name")
                .build();

        final Practitioner orgAPractitioner = (Practitioner) APITestHelpers.createResource(orgAClient, practitioner1).getResource();
        final Practitioner orgBPractitioner = (Practitioner) APITestHelpers.createResource(orgBClient, practitioner2).getResource();

        //Test PUT /Practitioner/{id}
        assertThrows(AuthenticationException.class,
                () -> APITestHelpers.updateResource(orgBClient, orgAPractitioner.getIdElement().getIdPart(), orgAPractitioner)
                , "Expected auth exception when updating another org's practitioner.");
        assertThrows(NotImplementedOperationException.class,
                () -> APITestHelpers.updateResource(orgAClient, orgAPractitioner.getIdElement().getIdPart(), orgAPractitioner)
                , "Expected Not Implemented exception when updating a practitioner.");
        assertThrows(NotImplementedOperationException.class,
                () -> APITestHelpers.updateResource(orgBClient, orgBPractitioner.getIdElement().getIdPart(), orgBPractitioner)
                , "Expected Not Implemented exception when updating a practitioner.");
    }

    @Test
    @DisplayName("Delete practitioners with invalid authorization 🤮")
    @Order(10)
    public void testDeletePractitionerPathInvalidAuthorization() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner practitioner1 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgAContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org A Practitioner", "Last name")
                .build();

        final Practitioner orgAPractitioner = (Practitioner) APITestHelpers.createResource(orgAClient, practitioner1).getResource();

        //Test DELETE /Practitioner/{id}
        assertThrows(AuthenticationException.class,
                () -> APITestHelpers.deleteResourceById(orgBClient, DPCResourceType.Practitioner, orgAPractitioner.getIdElement().getIdPart())
                , "Expected auth exception when deleting another org's practitioner.");
    }

    
    @Test
    @DisplayName("Delete practitioners 🥳")
    @Order(11)
    public void testDeletePractitionerPathAuthorization() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner practitioner1 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgAContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org A Practitioner", "Last name")
                .build();

        Practitioner practitioner2 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgBContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org B Practitioner", "Last name")
                .build();

        final Practitioner orgAPractitioner = (Practitioner) APITestHelpers.createResource(orgAClient, practitioner1).getResource();
        final Practitioner orgBPractitioner = (Practitioner) APITestHelpers.createResource(orgBClient, practitioner2).getResource();

        APITestHelpers.deleteResourceById(orgAClient, DPCResourceType.Practitioner, orgAPractitioner.getIdElement().getIdPart());
        APITestHelpers.deleteResourceById(orgBClient, DPCResourceType.Practitioner, orgBPractitioner.getIdElement().getIdPart());
    }

    @Test
    @DisplayName("Create and access practitioner with override of mismatching metadata 🥳")
    @Order(12)
    public void testRequestBodyForgery() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner practitioner = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgBContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org B Practitioner", "Last name")
                .build();

        //Test forgery during practitioner creation (Specify another org's id in the metadata tag)
        practitioner.getMeta().addTag(DPCIdentifierSystem.DPC.getSystem(), orgBContext.getOrgId(), "Organization ID");
        APITestHelpers.createResource(orgAClient, practitioner).getResource();

        Bundle bundle = APITestHelpers.resourceSearch(orgBClient, DPCResourceType.Practitioner);
        assertEquals(0, bundle.getTotal(), "Expected Org B to have 0 practitioners.");

        bundle = APITestHelpers.resourceSearch(orgAClient, DPCResourceType.Practitioner);
        assertEquals(1, bundle.getTotal(), "Expected Org A to have 1 practitioner.");
    }

    @Test
    @DisplayName("Create and access multiple practitioners with override of mismatching metadata 🥳")
    @Order(13)
    public void testRequestBodyForgeryOnMultipleSubmit() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner practitioner1 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgBContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org B Practitioner", "Last name")
                .build();

        Practitioner practitioner2 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgBContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org B Practitioner", "Last name")
                .build();

        Practitioner practitioner3 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgBContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org B Practitioner", "Last name")
                .build();

        Practitioner practitioner4 = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgBContext.getOrgId())
                .withNpi(NPIUtil.generateNPI())
                .withName("Org B Practitioner", "Last name")
                .build();

        practitioner4.getMeta().addTag(DPCIdentifierSystem.DPC.getSystem(), orgBContext.getOrgId(), "Organization ID");

        Bundle bundle = new Bundle();
        bundle.addEntry(new Bundle.BundleEntryComponent().setResource(practitioner1));
        bundle.addEntry(new Bundle.BundleEntryComponent().setResource(practitioner2));
        bundle.addEntry(new Bundle.BundleEntryComponent().setResource(practitioner3));
        bundle.addEntry(new Bundle.BundleEntryComponent().setResource(practitioner4));

        orgAClient.operation()
                .onType(Practitioner.class)
                .named("submit")
                .withParameter(Parameters.class, "name", bundle)
                .execute();


        //Test forgery during practitioner creation (Specify another org's id in the metadata tag)
        Bundle result = APITestHelpers.resourceSearch(orgBClient, DPCResourceType.Practitioner);
        assertEquals(0, result.getTotal(), "Expected Org B to have 0 practitioners.");

        result = APITestHelpers.resourceSearch(orgAClient, DPCResourceType.Practitioner);
        assertEquals(4, result.getTotal(), "Expected Org A to have 4 practitioners.");
    }
}
