package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.TestOrganizationContext;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.r4.FHIRExtractors;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.factories.r4.FHIRGroupBuilder;
import gov.cms.dpc.testing.utils.MBIUtil;
import jakarta.ws.rs.HttpMethod;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static gov.cms.dpc.api.APITestHelpers.V3.*;
import static org.junit.jupiter.api.Assertions.*;

@Disabled("until all resources are using R4")
@ExtendWith(BufferedLoggerHandler.class)
public class GroupResourceTest extends AbstractSecureApplicationTest {

    @Test
    void testMissingProvenance() throws IOException {
        final IParser parser = r4Ctx.newJsonParser();
        IGenericClient client = buildV3Client();
        setupPatientTest(client, parser);
        setupPractitionerTest(client, parser);

        // Create a patient
        final Bundle patientSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, patientSearch.getTotal(), "Should have a single patient");

        final Patient patient = (Patient) patientSearch.getEntryFirstRep().getResource();

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

        final Group group = FHIRGroupBuilder.newBuild()
                .attributedTo(FHIRExtractors.getProviderNPI(foundProvider))
                .withOrgTag(UUID.fromString(ORGANIZATION_ID))
                .withPatients(patient.getIdElement().getIdPart())
                .build();

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
        final Provenance provenance = createProvenance(
                ORGANIZATION_ID,
                "Practitioner/" + foundProvider.getIdElement().getIdPart(),
                List.of(patient.getIdElement().getValue()));

        creation.withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance));
        creation.execute();
    }

    @Test
    void testInvalidInputsWhenCreatingGroup() throws IOException {
        final IParser parser = r4Ctx.newJsonParser();
        IGenericClient client = buildV3Client();
        setupPatientTest(client, parser);
        setupPractitionerTest(client, parser);

        // Grab the created patient
        final Bundle patientSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, patientSearch.getTotal(), "Should have a single patient");

        final Patient patient = (Patient) patientSearch.getEntryFirstRep().getResource();

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

        final Group group = FHIRGroupBuilder.newBuild()
                .attributedTo(FHIRExtractors.getProviderNPI(foundProvider))
                .withOrgTag(UUID.fromString(ORGANIZATION_ID))
                .withPatients(patient.getIdElement().getValue())
                .build();

        final String safeCodeValue = group.getCharacteristicFirstRep().getCode().getCodingFirstRep().getCode();
        group.getCharacteristicFirstRep().getCode().getCodingFirstRep().setCode("<script>nope</script>");

        // Submit the group
        ICreateTyped invalidCreate = client
                .create()
                .resource(group)
                .encodedJson();
        assertThrows(InvalidRequestException.class, invalidCreate::execute);

        // Reset back to good safe value
        group.getCharacteristicFirstRep().getCode().getCodingFirstRep().setCode(safeCodeValue);

        // Test scripts in provenance
        final Provenance provenance = createProvenance(
                ORGANIZATION_ID,
                "Practitioner/" + foundProvider.getIdElement().getIdPart(),
                List.of(patient.getIdElement().getValue()));
        provenance.getReasonFirstRep().getCodingFirstRep().setCode("<script>nope</script>");

        // Provenance parser can't parse
        final ICreateTyped unprocessableCreate = client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance));
        assertThrows(UnprocessableEntityException.class, unprocessableCreate::execute);
    }

    @Test
    void testCreateInvalidGroup() throws IOException, URISyntaxException {
        Provenance provenance = new Provenance();
        provenance.setRecorded(java.sql.Date.valueOf(LocalDate.now()));
        provenance.addReason().addCoding(new Coding("http://hl7.org/fhir/v3/ActReason", "TREAT", null));
        String provString = r4Ctx.newJsonParser().encodeResourceToString(provenance);

        URL url = new URI(getV3BaseURL() + "/Group").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.POST);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setRequestProperty("X-Provenance", provString);

        APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

        conn.setDoOutput(true);
        final String reqBody = "{\"test\": \"test\"}";
        conn.getOutputStream().write(reqBody.getBytes());

        assertEquals(HttpStatus.BAD_REQUEST_400, conn.getResponseCode());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
            StringBuilder respBuilder = new StringBuilder();
            String respLine;
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
    void testCreateGroupReturnsAppropriateHeaders() throws IOException {
        final IParser parser = ctx.newJsonParser();
        IGenericClient client = buildV3Client();
        setupPatientTest(client, parser);
        setupPractitionerTest(client, parser);

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

        final Group group = FHIRGroupBuilder.newBuild()
                .attributedTo(FHIRExtractors.getProviderNPI(foundProvider))
                .withPatients(patient.getIdElement().getValue())
                .build();

        // Test scripts in provenance
        final Provenance provenance = createProvenance(
                ORGANIZATION_ID,
                "Practitioner/" + foundProvider.getIdElement().getIdPart(),
                List.of(patient.getIdElement().getValue()));

        MethodOutcome methodOutcome = client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance))
                .execute();

        String location = methodOutcome.getResponseHeaders().get("location").getFirst();
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
    void testProvenanceHeaderAndGroupProviderMatch() {
        IGenericClient client = buildV3Client();
        Practitioner practitioner = createPractitionerResource(NPIUtil.generateNPI(), ORGANIZATION_ID);

        MethodOutcome outcome = client.create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        Practitioner createdPractitioner = (Practitioner) outcome.getResource();

        // Group will have non-matching practitioner NPI
        Group group = FHIRGroupBuilder.newBuild()
                .attributedTo(NPIUtil.generateNPI())
                .withOrgTag(UUID.fromString(ORGANIZATION_ID))
                .build();

        final Provenance provenance = createProvenance(
                ORGANIZATION_ID,
                "Practitioner/" + createdPractitioner.getIdElement().getIdPart(),
                List.of());

        ICreateTyped createGroup = client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance));
        assertThrows(UnprocessableEntityException.class, createGroup::execute);

        // Create provenance with unknown practitioner
        group = FHIRGroupBuilder.newBuild()
                .attributedTo(FHIRExtractors.getProviderNPI(createdPractitioner))
                .build();
        Provenance unknownProvenance = createProvenance(
                ORGANIZATION_ID,
                "Practitioner/" + UUID.randomUUID(),
                List.of());

        createGroup = client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(unknownProvenance));
        Assertions.assertThrows(UnprocessableEntityException.class, createGroup::execute);

        client.delete()
                .resource(createdPractitioner)
                .encodedJson()
                .execute();
    }

    @Test
    void testGroupCanOnlyBeRetrievedByOwner() throws GeneralSecurityException, IOException, URISyntaxException, ParseException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = buildV3Client(orgAContext);
        final IGenericClient orgBClient = buildV3Client(orgBContext);

        Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgAClient);
        Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.emptyList());

        Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);
        Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        final String orgAGroupId = orgAGroup.getId();
        final String orgBGroupId = orgBGroup.getId();

        Group foundGroup = orgAClient.read()
                .resource(Group.class)
                .withId(orgAGroupId)
                .encodedJson()
                .execute();
        assertNotNull(foundGroup, "Org A should have been able to retrieve their own group");
        assertEquals(orgAGroupId, foundGroup.getIdElement().getIdPart(), "Returned group ID should have been the same as requested ID");

        foundGroup = orgBClient.read()
                .resource(Group.class)
                .withId(orgBGroupId)
                .encodedJson()
                .execute();
        assertNotNull(foundGroup, "Org B should have been able to retrieve their own group");
        assertEquals(orgBGroupId, foundGroup.getIdElement().getIdPart(), "Returned group ID should have been the same as requested ID");

        IReadExecutable<Group> notFoundRead = orgBClient.read()
                .resource(Group.class)
                .withId(orgAGroupId)
                .encodedJson();
        assertThrows(ResourceNotFoundException.class, notFoundRead::execute,
                "Organization B should not be able to retrieve group from another organization (Org A)");
    }

    @Test
    void testOrgCanOnlyUpdateTheirOwnGroup() throws GeneralSecurityException, IOException, URISyntaxException, ParseException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = buildV3Client(orgAContext);
        final IGenericClient orgBClient = buildV3Client(orgBContext);

        Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgAClient);
        Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.emptyList());

        Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);
        Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        Provenance provenance = createProvenance(
                orgBContext.getOrgId(),
                "Practitioner/" + orgBPractitioner.getIdElement().getIdPart(),
                List.of());
        IUpdateExecutable notFoundUpdate = orgBClient
                .update()
                .resource(orgAGroup)
                .withId(orgAGroup.getId())
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance));
        assertThrows(ResourceNotFoundException.class, notFoundUpdate::execute,
                "Org should receive auth error when updating another org's group.");

        MethodOutcome response = orgBClient.update().resource(orgBGroup)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance))
                .execute();

        assertNotNull(response.getResource(), "Org B should have been able to update their group");
    }

    @Test
    void testOrgCanOnlyAddPatientsTheyManageToGroup() throws IOException, URISyntaxException, GeneralSecurityException, ParseException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = buildV3Client(orgAContext);
        final IGenericClient orgBClient = buildV3Client(orgBContext);

        // Setup org A with a patient
        final Patient orgAPatient = createAndSubmitPatient(orgAContext.getOrgId(), orgAClient);

        // Setup org B with a practitioner and empty group
        final Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBClient);
        final Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        // Ensure Org B can not add Org A's patient to group using add operation:  /Group/{id}/$member-add
        orgBGroup.addMember(new Group.GroupMemberComponent().setEntity(new Reference(orgAPatient.getId())));

        URL url = new URI(getV3BaseURL() + "/Group/" + orgBGroup.getId() + "/$member-add").toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.POST);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setDoOutput(true);

        APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

        Provenance provenance = createProvenance(
                orgBContext.getOrgId(),
                "Practitioner/" + orgBPractitioner.getIdElement().getIdPart(),
                List.of());
        conn.setRequestProperty("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance));

        String reqBody = r4Ctx.newJsonParser().encodeResourceToString(orgBGroup);
        conn.getOutputStream().write(reqBody.getBytes());

        assertEquals(HttpStatus.BAD_REQUEST_400, conn.getResponseCode());
        String body = new String(conn.getErrorStream().readAllBytes());
        assertTrue(body.contains("All patients in group must exist"));

        conn.disconnect();
    }

    private Patient createAndSubmitPatient(String orgId, IGenericClient client) {
        final Patient patient = createPatientResource(MBIUtil.generateMBI(), orgId);
        final MethodOutcome methodOutcome = client.create()
                .resource(patient)
                .encodedJson()
                .execute();
        return (Patient) methodOutcome.getResource();
    }

    private Practitioner createAndSubmitPractitioner(String orgId, IGenericClient client) {
        final Practitioner practitioner = createPractitionerResource(NPIUtil.generateNPI(), orgId);
        final MethodOutcome methodOutcome = client.create()
                .resource(practitioner)
                .encodedJson()
                .execute();
        return (Practitioner) methodOutcome.getResource();
    }

    private Group createAndSubmitGroup(String orgId, Practitioner practitioner, IGenericClient client, List<String> patientIds) {
        final String practitionerId = practitioner.getId();
        final String practitionerNPI = practitioner.getIdentifierFirstRep().getValue();

        final Group group = FHIRGroupBuilder.newBuild()
                .attributedTo(practitionerNPI)
                .withOrgTag(UUID.fromString(orgId))
                .withPatients(patientIds.toArray(String[]::new))
                .build();

        final Provenance provenance = createProvenance(orgId, practitionerId, patientIds);

        final MethodOutcome outcome = client.create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", ctx.newJsonParser().encodeResourceToString(provenance))
                .execute();

        return (Group) outcome.getResource();
    }
}
