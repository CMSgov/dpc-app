package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.TestOrganizationContext;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.factories.FHIRGroupBuilder;
import org.apache.http.HttpHeaders;
import org.assertj.core.util.Lists;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.V3RoleClass;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static gov.cms.dpc.api.APITestHelpers.createProvenance;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
public class GroupResourceTest extends AbstractSecureApplicationTest {

    @Test
    void testMissingProvenance() throws IOException {
        final IParser parser = ctx.newJsonParser();
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        APITestHelpers.setupPractitionerTest(client, parser);

        // Create a patient
        final Bundle specificSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, specificSearch.getTotal(), "Should have a single patient");

        final Patient patient = (Patient) specificSearch.getEntryFirstRep().getResource();

        // Create the practitioner
        final Bundle practSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("1232131239"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, practSearch.getTotal(), "Should have a specific provider");

        // Fetch the provider directly
        final Practitioner foundProvider = (Practitioner) practSearch.getEntryFirstRep().getResource();


        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(foundProvider), ORGANIZATION_ID);

        final Reference patientRef = new Reference(patient.getIdElement());
        group.addMember().setEntity(patientRef);

        // Submit the group

        final ICreateTyped creation = client
                .create()
                .resource(group)
                .encodedJson();

        final InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, creation::execute, "Should throw a 400");
        final OperationOutcome operationOutcome = (OperationOutcome) invalidRequestException.getOperationOutcome();
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, invalidRequestException.getStatusCode(), "Should have 400 status"),
                () -> assertEquals("Must have X-Provenance header", operationOutcome.getIssueFirstRep().getDetails().getText(), "Should have correct message"));

        // Try again with provenance
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()));
        final Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/v3/ActReason");
        coding.setCode("TREAT");
        provenance.setReason(Collections.singletonList(coding));
        provenance.setTarget(Collections.singletonList(patientRef));
        final Provenance.ProvenanceAgentComponent component = new Provenance.ProvenanceAgentComponent();

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        component.setRole(Collections.singletonList(roleConcept));
        component.setWho(new Reference(new IdType("Organization", ORGANIZATION_ID)));
        component
                .setOnBehalfOf(new Reference(foundProvider.getIdElement()));

        provenance.addAgent(component);

        creation
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance));

        creation.execute();
    }

    @Test
    public void testInvalidInputsWhenCreatingGroup() throws IOException {
        final IParser parser = ctx.newJsonParser();
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        APITestHelpers.setupPractitionerTest(client, parser);

        // Grab the created patient
        final Bundle specificSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, specificSearch.getTotal(), "Should have a single patient");

        final Patient patient = (Patient) specificSearch.getEntryFirstRep().getResource();

        // Grab the created practitioner
        final Bundle practSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("1234329724"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, practSearch.getTotal(), "Should have a specific provider");

        // Fetch the provider directly
        final Practitioner foundProvider = (Practitioner) practSearch.getEntryFirstRep().getResource();

        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(foundProvider), ORGANIZATION_ID);

        final Reference patientRef = new Reference(patient.getIdElement());
        group.addMember().setEntity(patientRef);

        String safeCodeValue = group.getCharacteristicFirstRep().getCode().getCodingFirstRep().getCode();
        group.getCharacteristicFirstRep().getCode().getCodingFirstRep().setCode("<script>nope</script>");

        // Submit the group
        assertThrows(InvalidRequestException.class, () -> client
                .create()
                .resource(group)
                .encodedJson()
                .execute());

        // Reset back to good safe value
        group.getCharacteristicFirstRep().getCode().getCodingFirstRep().setCode(safeCodeValue);

        // Test scripts in provenance
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()));
        final Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/v3/ActReason");
        coding.setCode("<script>nope</script>");
        provenance.setReason(Collections.singletonList(coding));
        provenance.setTarget(Collections.singletonList(patientRef));
        final Provenance.ProvenanceAgentComponent component = new Provenance.ProvenanceAgentComponent();

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        component.setRole(Collections.singletonList(roleConcept));
        component.setWho(new Reference(new IdType("Organization", ORGANIZATION_ID)));
        component
                .setOnBehalfOf(new Reference(foundProvider.getIdElement()));

        provenance.addAgent(component);

        //Provenance parser can't parse
        assertThrows(UnprocessableEntityException.class, () -> client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))
                .execute());
    }

    @Test
    void testCreateInvalidGroup() throws IOException, URISyntaxException {
        Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf(LocalDate.now()));
        provenance.setReason(List.of(new Coding("http://hl7.org/fhir/v3/ActReason", "TREAT", null)));
        String provString = ctx.newJsonParser().encodeResourceToString(provenance);

        URL url = new URL(getBaseURL() + "/Group");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.POST);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setRequestProperty("X-Provenance", provString);

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
            assertAll(() -> assertTrue(resp.contains("\"resourceType\":\"OperationOutcome\"")),
                    () -> assertTrue(resp.contains("Invalid JSON content")));
        }

        conn.disconnect();
    }

    @Test
    public void testCreateGroupReturnsAppropriateHeaders() throws IOException {
        final IParser parser = ctx.newJsonParser();
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        APITestHelpers.setupPractitionerTest(client, parser);

        // Grab the created patient
        final Bundle specificSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, specificSearch.getTotal(), "Should have a single patient");

        final Patient patient = (Patient) specificSearch.getEntryFirstRep().getResource();

        // Grab the created practitioner
        final Bundle practSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("1234329724"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, practSearch.getTotal(), "Should have a specific provider");

        // Fetch the provider directly
        final Practitioner foundProvider = (Practitioner) practSearch.getEntryFirstRep().getResource();

        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(foundProvider), ORGANIZATION_ID);

        final Reference patientRef = new Reference(patient.getIdElement());
        group.addMember().setEntity(patientRef);


        // Test scripts in provenance
        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()));
        final Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/v3/ActReason");
        coding.setCode("TREAT");
        provenance.setReason(Collections.singletonList(coding));
        provenance.setTarget(Collections.singletonList(patientRef));
        final Provenance.ProvenanceAgentComponent component = new Provenance.ProvenanceAgentComponent();

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        component.setRole(Collections.singletonList(roleConcept));
        component.setWho(new Reference(new IdType("Organization", ORGANIZATION_ID)));
        component
                .setOnBehalfOf(new Reference(foundProvider.getIdElement()));

        provenance.addAgent(component);

        MethodOutcome methodOutcome = client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))
                .execute();

        String location = methodOutcome.getResponseHeaders().get("location").get(0);
        assertNotNull(location);

        Group foundGroup = client.read()
                .resource(Group.class)
                .withUrl(location)
                .encodedJson()
                .execute();

        assertEquals(group.getIdentifierFirstRep().getValue(), foundGroup.getIdentifierFirstRep().getValue());

        client.delete()
                .resource(foundGroup)
                .encodedJson()
                .execute();
    }

    @Test
    public void testProvenanceHeaderAndGroupProviderMatch() {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        Practitioner practitioner = APITestHelpers.createPractitionerResource(NPIUtil.generateNPI(), ORGANIZATION_ID);

        MethodOutcome methodOutcome = client.create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        Practitioner createdPractitioner = (Practitioner) methodOutcome.getResource();

        //Group will have non-matching practitioner NPI
        Group group = SeedProcessor.createBaseAttributionGroup(NPIUtil.generateNPI(), ORGANIZATION_ID);

        final Provenance provenance = new Provenance();
        provenance.setRecorded(Date.valueOf(Instant.now().atZone(ZoneOffset.UTC).toLocalDate()));
        final Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/v3/ActReason");
        coding.setCode("TREAT");
        provenance.setReason(Collections.singletonList(coding));
        final Provenance.ProvenanceAgentComponent component = new Provenance.ProvenanceAgentComponent();

        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        component.setRole(Collections.singletonList(roleConcept));
        component.setWho(new Reference(new IdType("Organization", ORGANIZATION_ID)));
        component.setOnBehalfOf(new Reference(createdPractitioner.getIdElement()));

        provenance.addAgent(component);

        ICreateTyped createGroup = client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance));

        Assertions.assertThrows(UnprocessableEntityException.class, createGroup::execute);

        group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(createdPractitioner), ORGANIZATION_ID);
        //set provenance practitioner to unknown practitioner;
        component.setOnBehalfOf(new Reference(new IdType("Practitioner", UUID.randomUUID().toString())));

        createGroup = client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance));

        Assertions.assertThrows(UnprocessableEntityException.class, createGroup::execute);

        client.delete()
                .resource(createdPractitioner)
                .encodedJson()
                .execute();
    }

    @Test
    public void testGroupCanOnlyBeRetrievedByOwner() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgAClient);
        Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.emptyList());

        Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);
        Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        Group foundGroup = orgAClient.read()
                .resource(Group.class)
                .withId(orgAGroup.getId())
                .encodedJson()
                .execute();

        assertNotNull(foundGroup, "Org A should have been able to retrieve their own group");
        assertEquals(orgAGroup.getId(), foundGroup.getId(), "Returned group ID should have been the same as requested ID");

        foundGroup = orgBClient.read()
                .resource(Group.class)
                .withId(orgBGroup.getId())
                .encodedJson()
                .execute();

        assertNotNull(foundGroup, "Org B should have been able to retrieve their own group");
        assertEquals(orgBGroup.getId(), foundGroup.getId(), "Returned group ID should have been the same as requested ID");


        assertThrows(AuthenticationException.class, () -> {
            orgBClient.read()
                    .resource(Group.class)
                    .withId(orgAGroup.getId())
                    .encodedJson()
                    .execute();
        },"Organization B should not be able to retrieve group from another organization (Org A)");
    }

    @Test
    public void testOrgCanOnlyDeleteTheirOwnGroup() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgAClient);
        Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.emptyList());

        Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);
        Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        Provenance provenance = APITestHelpers.createProvenance(orgBContext.getOrgId(), orgBPractitioner.getId(), Collections.emptyList());
        assertThrows(AuthenticationException.class, () -> {
            orgBClient.delete()
                    .resource(orgAGroup)
                    .encodedJson()
                    .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))
                    .execute();
        }, "Organization should not be able to delete another organization's group.");

        IBaseOperationOutcome result = orgBClient
                .delete()
                .resource(orgBGroup)
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))
                .execute();
        assertNull(result, "Organization should have been able to delete their own group.");
    }

    @Test
    public void testOrgCanOnlyUpdateTheirOwnGroup() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgAClient);
        Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.emptyList());

        Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);
        Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        Provenance provenance = APITestHelpers.createProvenance(orgBContext.getOrgId(), orgBPractitioner.getId(), Collections.emptyList());
        assertThrows(AuthenticationException.class, () -> {
            orgBClient.update().resource(orgAGroup)
                    .encodedJson()
                    .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))
                    .execute();
        }, "Org should receive auth error when updating another org's group.");

        MethodOutcome response = orgBClient.update().resource(orgBGroup)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))
                .execute();

        assertNotNull(response.getResource(), "Org B should have been able to retrieve their group");
    }

    @Test
    public void testOrgCanOnlyListTheirOwnGroups() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgAClient);
        Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.emptyList());

        Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);
        createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        Bundle results = orgAClient.search()
                .forResource(DPCResourceType.Group.toString())
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, results.getTotal(), "Org A should only have one result.");
        assertEquals(orgAGroup.getId(), results.getEntryFirstRep().getResource().getId(), "Group returned should match uploaded group");
    }


    @Test
    public void testOrgCanOnlyCreateGroupWithPatientsTheyManage() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        //Setup Org A with a practitioner and patient.
        final Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgAClient);
        final Patient orgAPatient = (Patient) APITestHelpers.createResource(orgAClient, APITestHelpers.createPatientResource("4S41C00AA00", orgAContext.getOrgId())).getResource();
        assertNotNull(orgAPatient, "Patient should have been created");

        //Setup OrgB with a practitioner
        final Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);

        //Assert org A can add their own patient to roster successfully.
        final String orgAPatientId = orgAPatient.getId();
        final Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.singletonList(orgAPatientId));
        assertNotNull(orgAGroup, "Roster for Org A should have been created with patient they manage.");

        //Assert Org B can NOT add OrgA's patient
        InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> {
            createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.singletonList(orgAPatientId));
        }, "Org should not be able to add patients they do not managed to their group.");

        assertTrue(e.getResponseBody().contains("Cannot find patient with ID " + new IdType(orgAPatient.getId()).getIdPart()));
    }

    @Test
    public void testOrgCanOnlyUpdateGroupWithPatientsTheyManage() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        //Setup Org A with a practitioner, patient, and group.
        final Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgAClient);
        final Patient orgAPatient = (Patient) APITestHelpers.createResource(orgAClient, APITestHelpers.createPatientResource("4S41C00AA00", orgAContext.getOrgId())).getResource();
        final Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.singletonList(orgAPatient.getId()));
        assertNotNull(orgAGroup, "Org should have been able to create group with patient they manage.");

        //Setup Org B with a practitioner, and empty group.
        final Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);
        final Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        //Assert an org can NOT update their group with another org's patient.
        InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> {
            orgBGroup.addMember(new Group.GroupMemberComponent().setEntity(new Reference(orgAPatient.getId())));
            Provenance provenance = APITestHelpers.createProvenance(orgBContext.getOrgId(), orgBPractitioner.getId(), Collections.emptyList());
            String provenanceStr = ctx.newJsonParser().encodeResourceToString(provenance);
            APITestHelpers.updateResource(orgBClient, orgBGroup.getId(), orgBGroup, Map.of("X-Provenance", provenanceStr));
        }, "Org B should not be able to update roster with patient managed by Org A ");

        assertTrue(e.getResponseBody().contains("Cannot find patient with ID " + new IdType(orgAPatient.getId()).getIdPart()));
    }


    @Test
    public void testOrgCanOnlyAddPatientsTheyManageToGroup() throws IOException, URISyntaxException, GeneralSecurityException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        //Setup org A with a patient
        final Patient orgAPatient = (Patient) APITestHelpers.createResource(orgAClient, APITestHelpers.createPatientResource("4S41C00AA00", orgAContext.getOrgId())).getResource();

        //Setup org B with a practitioner and empty group.
        final Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);
        final Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        //Ensure Org B can not add Org A's patient to group using add operation:  /Group/{id}/$add
        orgBGroup.addMember(new Group.GroupMemberComponent().setEntity(new Reference(orgAPatient.getId())));


        URL url = new URL(getBaseURL() + "/" + orgBGroup.getId() + "/$add");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.POST);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setDoOutput(true);

        APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

        Provenance provenance = createProvenance(orgBContext.getOrgId(), orgBPractitioner.getId(), Collections.emptyList());
        String provString = ctx.newJsonParser().encodeResourceToString(provenance);
        conn.setRequestProperty("X-Provenance", provString);

        String reqBody = ctx.newJsonParser().encodeResourceToString(orgBGroup);
        conn.getOutputStream().write(reqBody.getBytes());

        assertEquals(HttpStatus.BAD_REQUEST_400, conn.getResponseCode());
        String body = new String(conn.getErrorStream().readAllBytes());
        assertTrue(body.contains("Cannot find patient with ID " + new IdType(orgAPatient.getId()).getIdPart()));

        conn.disconnect();
    }

    @Test
    public void testApiDoesNotUseOrgTagSpecifiedByClient() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        //Setup Org A with a practitioner.
        final Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgAClient);
        final Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.emptyList());

        //Setup OrgB with a practitioner.
        final Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);
        final Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        Group groupWithOrgATag = FHIRGroupBuilder.newBuild()
                .attributedTo(orgBPractitioner.getIdentifierFirstRep().getValue())
                .withOrgTag(UUID.fromString(orgAContext.getOrgId()))
                .build();

        Provenance provenance = APITestHelpers.createProvenance(orgBContext.getOrgId(), orgBPractitioner.getId(), Collections.emptyList());
        Group result = (Group) APITestHelpers.createResource(orgBClient, groupWithOrgATag, Map.of("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))).getResource();

        assertEquals(orgBGroup.getId(),result.getId(), "Org B's group should have been returned even if they specified Org A in the meta tag");
    }


    private Practitioner createAndSubmitPractitioner(String orgId, IGenericClient client) {
        Practitioner practitioner = APITestHelpers.createPractitionerResource(NPIUtil.generateNPI(), orgId);
        MethodOutcome methodOutcome = client.create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        return (Practitioner) methodOutcome.getResource();
    }

    private Group createAndSubmitGroup(String orgId, Practitioner practitioner, IGenericClient client, List<String> patientIds) {
        final String practitionerId = practitioner.getId();
        final String practitionerNPI = practitioner.getIdentifierFirstRep().getValue();
        Group group = FHIRGroupBuilder.newBuild()
                .attributedTo(practitionerNPI)
                .withOrgTag(UUID.fromString(orgId))
                .withPatients(patientIds.toArray(String[]::new))
                .build();

        Provenance provenance = APITestHelpers.createProvenance(orgId, practitionerId, Collections.emptyList());
        return (Group) APITestHelpers.createResource(client, group, Map.of("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))).getResource();
    }
}