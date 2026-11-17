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
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.TestOrganizationContext;
import gov.cms.dpc.api.V3APITestHelpers;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.factories.r4.FHIRGroupBuilder;
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
import static org.junit.jupiter.api.Assertions.*;

// Uses R4 for Group and STU3 for Patient/Practitioner, can be updated when those classes are
@Disabled(value = "blocked on R4 attribution client")
@ExtendWith(BufferedLoggerHandler.class)
public class GroupResourceTest extends AbstractSecureApplicationTest {

    @Test
    void testMissingProvenance() throws IOException {
        final IGenericClient setupClient = buildSetupClient();
        final IGenericClient v3Client = buildV3Client();
        final IParser stu3Parser = ctx.newJsonParser();

        APITestHelpers.setupPatientTest(setupClient, stu3Parser);
        APITestHelpers.setupPractitionerTest(setupClient, stu3Parser);

        // Create a patient
        final org.hl7.fhir.dstu3.model.Bundle patientSearch = setupClient
                .search()
                .forResource(org.hl7.fhir.dstu3.model.Patient.class)
                .where(org.hl7.fhir.dstu3.model.Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(org.hl7.fhir.dstu3.model.Bundle.class)
                .encodedJson()
                .execute();
        assertEquals(1, patientSearch.getTotal(), "Should have a single patient");
        final org.hl7.fhir.dstu3.model.Patient patient = (org.hl7.fhir.dstu3.model.Patient) patientSearch.getEntryFirstRep().getResource();

        // Create the practitioner
        final org.hl7.fhir.dstu3.model.Bundle practSearch = setupClient
                .search()
                .forResource(org.hl7.fhir.dstu3.model.Practitioner.class)
                .where(org.hl7.fhir.dstu3.model.Practitioner.IDENTIFIER.exactly().code("1232131239"))
                .returnBundle(org.hl7.fhir.dstu3.model.Bundle.class)
                .encodedJson()
                .execute();
        assertEquals(1, practSearch.getTotal(), "Should have a specific provider");
        final org.hl7.fhir.dstu3.model.Practitioner foundProvider = (org.hl7.fhir.dstu3.model.Practitioner) practSearch.getEntryFirstRep().getResource();

        // Fetch the provider directly
        final String providerNPI = foundProvider.getIdentifierFirstRep().getValue();
        final Group group = FHIRGroupBuilder.newBuild()
                .attributedTo(providerNPI)
                .withPatients(patient.getIdElement().getValue())
                .build();

        // Submit the group
        final ICreateTyped creation = v3Client
                .create()
                .resource(group)
                .encodedJson();

        final InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, creation::execute, "Should throw a 400");
        final OperationOutcome operationOutcome = (OperationOutcome) invalidRequestException.getOperationOutcome();
        assertAll(() -> assertEquals(HttpStatus.BAD_REQUEST_400, invalidRequestException.getStatusCode(), "Should have 400 status"),
                () -> assertEquals("Must have X-Provenance header", operationOutcome.getIssueFirstRep().getDetails().getText(), "Should have correct message"));

        // Try again with provenance
        final Provenance provenance = V3APITestHelpers.createProvenance(
                ORGANIZATION_ID, "Practitioner/" + foundProvider.getIdElement().getIdPart(),
                List.of(patient.getIdElement().getValue()));

        creation.withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance));
        creation.execute();
    }

    @Test
    void testInvalidInputsWhenCreatingGroup() throws IOException {
        final IGenericClient setupClient = buildSetupClient();
        final IGenericClient v3Client = buildV3Client();
        final IParser stu3Parser = ctx.newJsonParser();

        APITestHelpers.setupPatientTest(setupClient, stu3Parser);
        APITestHelpers.setupPractitionerTest(setupClient, stu3Parser);

        // Grab the created patient
        final org.hl7.fhir.dstu3.model.Bundle patientSearch = setupClient
                .search()
                .forResource(org.hl7.fhir.dstu3.model.Patient.class)
                .where(org.hl7.fhir.dstu3.model.Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(org.hl7.fhir.dstu3.model.Bundle.class)
                .encodedJson()
                .execute();
        assertEquals(1, patientSearch.getTotal(), "Should have a single patient");
        final org.hl7.fhir.dstu3.model.Patient patient = (org.hl7.fhir.dstu3.model.Patient) patientSearch.getEntryFirstRep().getResource();

        // Grab the created practitioner
        final org.hl7.fhir.dstu3.model.Bundle practSearch = setupClient
                .search()
                .forResource(org.hl7.fhir.dstu3.model.Practitioner.class)
                .where(org.hl7.fhir.dstu3.model.Practitioner.IDENTIFIER.exactly().code("1234329724"))
                .returnBundle(org.hl7.fhir.dstu3.model.Bundle.class)
                .encodedJson()
                .execute();
        assertEquals(1, practSearch.getTotal(), "Should have a specific provider");

        // Fetch the provider directly
        final org.hl7.fhir.dstu3.model.Practitioner foundProvider = (org.hl7.fhir.dstu3.model.Practitioner) practSearch.getEntryFirstRep().getResource();
        final String providerNPI = foundProvider.getIdentifierFirstRep().getValue();
        final Group group = FHIRGroupBuilder.newBuild()
                .attributedTo(providerNPI)
                .withPatients(patient.getIdElement().getValue())
                .build();

        final String safeCodeValue = group.getCharacteristicFirstRep().getCode().getCodingFirstRep().getCode();
        group.getCharacteristicFirstRep().getCode().getCodingFirstRep().setCode("<script>nope</script>");

        // Submit the group
        ICreateTyped invalidCreate = v3Client
                .create()
                .resource(group)
                .encodedJson();
        assertThrows(InvalidRequestException.class, invalidCreate::execute);

        // Reset back to good safe value
        group.getCharacteristicFirstRep().getCode().getCodingFirstRep().setCode(safeCodeValue);

        // Test scripts in provenance
        final Provenance provenance = V3APITestHelpers.createProvenance(
                ORGANIZATION_ID, "Practitioner/" + foundProvider.getIdElement().getIdPart(),
                List.of(patient.getIdElement().getValue()));
        provenance.getReasonFirstRep().getCodingFirstRep().setCode("<script>nope</script>");

        // Provenance parser can't parse
        final ICreateTyped unprocessableCreate = v3Client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance));
        assertThrows(UnprocessableEntityException.class, unprocessableCreate::execute);
    }

    @Test
    void testCreateInvalidGroup() throws IOException, URISyntaxException {
        final Provenance provenance = new Provenance();
        provenance.setRecorded(java.sql.Date.valueOf(LocalDate.now()));
        provenance.addReason().addCoding(new Coding("http://hl7.org/fhir/v3/ActReason", "TREAT", null));
        final String provString = r4Ctx.newJsonParser().encodeResourceToString(provenance);

        final URL url = new URI(getV3BaseURL() + "/Group").toURL();
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.POST);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setRequestProperty("X-Provenance", provString);

        final APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
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
            final String resp = respBuilder.toString();
            assertAll(() -> assertTrue(resp.contains("\"resourceType\":\"OperationOutcome\"")),
                    () -> assertTrue(resp.contains("Invalid JSON content")));
        }

        conn.disconnect();
    }

    @Test
    void testCreateGroupReturnsAppropriateHeaders() throws IOException {
        final IGenericClient setupClient = buildSetupClient();
        final IGenericClient v3Client = buildV3Client();
        final IParser stu3Parser = ctx.newJsonParser();

        APITestHelpers.setupPatientTest(setupClient, stu3Parser);
        APITestHelpers.setupPractitionerTest(setupClient, stu3Parser);

        // Grab the created patient
        final org.hl7.fhir.dstu3.model.Bundle patientSearch = setupClient
                .search()
                .forResource(org.hl7.fhir.dstu3.model.Patient.class)
                .where(org.hl7.fhir.dstu3.model.Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), "4S41C00AA00"))
                .returnBundle(org.hl7.fhir.dstu3.model.Bundle.class)
                .encodedJson()
                .execute();
        assertEquals(1, patientSearch.getTotal(), "Should have a single patient");
        final org.hl7.fhir.dstu3.model.Patient patient = (org.hl7.fhir.dstu3.model.Patient) patientSearch.getEntryFirstRep().getResource();

        // Grab the created practitioner
        final org.hl7.fhir.dstu3.model.Bundle practSearch = setupClient
                .search()
                .forResource(org.hl7.fhir.dstu3.model.Practitioner.class)
                .where(org.hl7.fhir.dstu3.model.Practitioner.IDENTIFIER.exactly().code("1234329724"))
                .returnBundle(org.hl7.fhir.dstu3.model.Bundle.class)
                .encodedJson()
                .execute();
        assertEquals(1, practSearch.getTotal(), "Should have a specific provider");

        // Fetch the provider directly
        final org.hl7.fhir.dstu3.model.Practitioner foundProvider = (org.hl7.fhir.dstu3.model.Practitioner) practSearch.getEntryFirstRep().getResource();
        final String providerNPI = foundProvider.getIdentifierFirstRep().getValue();
        final Group group = FHIRGroupBuilder.newBuild()
                .attributedTo(providerNPI)
                .withPatients(patient.getIdElement().getValue())
                .build();

        // Test scripts in provenance
        final Provenance provenance = V3APITestHelpers.createProvenance(
                ORGANIZATION_ID, "Practitioner/" + foundProvider.getIdElement().getIdPart(),
                List.of(patient.getIdElement().getValue()));

        final MethodOutcome methodOutcome = v3Client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance))
                .execute();

        final String location = methodOutcome.getResponseHeaders().get("location").getFirst();
        assertNotNull(location);

        final Group foundGroup = v3Client.read()
                .resource(Group.class)
                .withUrl(location)
                .encodedJson()
                .execute();

        assertEquals(group.getIdentifierFirstRep().getValue(), foundGroup.getIdentifierFirstRep().getValue());
    }

    @Test
    void testProvenanceHeaderAndGroupProviderMatch() {
        final IGenericClient setupClient = buildSetupClient();
        final IGenericClient v3Client = buildV3Client();

        final org.hl7.fhir.dstu3.model.Practitioner practitioner = APITestHelpers.createPractitionerResource(NPIUtil.generateNPI(), ORGANIZATION_ID);
        final MethodOutcome outcome = setupClient.create()
                .resource(practitioner)
                .encodedJson()
                .execute();
        final org.hl7.fhir.dstu3.model.Practitioner createdPractitioner = (org.hl7.fhir.dstu3.model.Practitioner) outcome.getResource();

        // Group will have non-matching practitioner NPI
        Group group = FHIRGroupBuilder.newBuild().attributedTo(NPIUtil.generateNPI()).build();

        Provenance provenance = V3APITestHelpers.createProvenance(
                ORGANIZATION_ID, "Practitioner/" + createdPractitioner.getIdElement().getIdPart(), List.of());

        ICreateTyped createGroup = v3Client
                .create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance));
        Assertions.assertThrows(UnprocessableEntityException.class, createGroup::execute);

        // Set provenance practitioner to unknown practitioner
        group = FHIRGroupBuilder.newBuild()
                .attributedTo(createdPractitioner.getIdentifierFirstRep().getValue())
                .build();
        provenance = V3APITestHelpers.createProvenance(
                ORGANIZATION_ID, "Practitioner/" + UUID.randomUUID(), List.of());

        final Group finalGroup = group;
        createGroup = v3Client
                .create()
                .resource(finalGroup)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance));
        Assertions.assertThrows(UnprocessableEntityException.class, createGroup::execute);
    }

    @Test
    void testGroupCanOnlyBeRetrievedByOwner() throws GeneralSecurityException, IOException, URISyntaxException, ParseException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgASetupClient = buildSetupClient(orgAContext);
        final IGenericClient orgBSetupClient = buildSetupClient(orgBContext);
        final IGenericClient orgAClient = buildV3Client(orgAContext);
        final IGenericClient orgBClient = buildV3Client(orgBContext);

        org.hl7.fhir.dstu3.model.Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgASetupClient);
        Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.emptyList());

        org.hl7.fhir.dstu3.model.Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBSetupClient);
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

        IReadExecutable<Group> notFoundRead = orgBClient.read()
                .resource(Group.class)
                .withId(orgAGroup.getId())
                .encodedJson();
        assertThrows(ResourceNotFoundException.class, notFoundRead::execute,
                "Organization B should not be able to retrieve group from another organization (Org A)");
    }

    @Test
    void testOrgCanOnlyUpdateTheirOwnGroup() throws GeneralSecurityException, IOException, URISyntaxException, ParseException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgASetupClient = buildSetupClient(orgAContext);
        final IGenericClient orgBSetupClient = buildSetupClient(orgBContext);
        final IGenericClient orgAClient = buildV3Client(orgAContext);
        final IGenericClient orgBClient = buildV3Client(orgBContext);

        org.hl7.fhir.dstu3.model.Practitioner orgAPractitioner = createAndSubmitPractitioner(orgAContext.getOrgId(), orgASetupClient);
        Group orgAGroup = createAndSubmitGroup(orgAContext.getOrgId(), orgAPractitioner, orgAClient, Collections.emptyList());

        org.hl7.fhir.dstu3.model.Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBSetupClient);
        Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        final Provenance provenance = V3APITestHelpers.createProvenance(
                orgBContext.getOrgId(), "Practitioner/" + orgBPractitioner.getIdElement().getIdPart(), List.of());
        final String provenanceStr = r4Ctx.newJsonParser().encodeResourceToString(provenance);

        IUpdateExecutable notFoundUpdate = orgBClient
                .update()
                .resource(orgAGroup)
                .withId(orgAGroup.getId())
                .encodedJson()
                .withAdditionalHeader("X-Provenance", provenanceStr);
        assertThrows(ResourceNotFoundException.class, notFoundUpdate::execute,
                "Org should receive auth error when updating another org's group.");

        final MethodOutcome response = orgBClient.update().resource(orgBGroup)
                .withId(orgBGroup.getId())
                .encodedJson()
                .withAdditionalHeader("X-Provenance", provenanceStr)
                .execute();
        assertNotNull(response.getResource(), "Org B should have been able to update their group");
    }

    @Test
    void testOrgCanOnlyAddPatientsTheyManageToGroup() throws IOException, URISyntaxException, GeneralSecurityException, ParseException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgASetupClient = buildSetupClient(orgAContext);
        final IGenericClient orgBSetupClient = buildSetupClient(orgBContext);
        final IGenericClient orgBClient = buildV3Client(orgBContext);

        // Setup org A with a patient
        final org.hl7.fhir.dstu3.model.Patient orgAPatient = (org.hl7.fhir.dstu3.model.Patient)
                APITestHelpers.createResource(orgASetupClient, APITestHelpers.createPatientResource("4S41C00AA00", orgAContext.getOrgId())).getResource();

        // Setup org B with a practitioner and empty group
        org.hl7.fhir.dstu3.model.Practitioner orgBPractitioner = createAndSubmitPractitioner(orgBContext.getOrgId(), orgBSetupClient);
        Group orgBGroup = createAndSubmitGroup(orgBContext.getOrgId(), orgBPractitioner, orgBClient, Collections.emptyList());

        // Ensure Org B can not add Org A's patient to group using add operation:  /Group/{id}/$add
        orgBGroup.addMember(new Group.GroupMemberComponent().setEntity(new Reference(orgAPatient.getId())));

        final URL url = new URI(getV3BaseURL() + "/Group/" + orgBGroup.getId() + "/$member-add").toURL();
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(HttpMethod.POST);
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "application/fhir+json");
        conn.setDoOutput(true);

        final APIAuthHelpers.AuthResponse auth = APIAuthHelpers.jwtAuthFlow(getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken);

        final Provenance provenance = V3APITestHelpers.createProvenance(
                orgBContext.getOrgId(), "Practitioner/" + orgBPractitioner.getIdElement().getIdPart(), List.of());
        conn.setRequestProperty("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance));

        final String reqBody = r4Ctx.newJsonParser().encodeResourceToString(orgBGroup);
        conn.getOutputStream().write(reqBody.getBytes());

        assertEquals(HttpStatus.BAD_REQUEST_400, conn.getResponseCode());
        final String body = new String(conn.getErrorStream().readAllBytes());
        assertTrue(body.contains("All patients in group must exist"));

        conn.disconnect();
    }

    private org.hl7.fhir.dstu3.model.Practitioner createAndSubmitPractitioner(String orgId, IGenericClient setupClient) {
        final org.hl7.fhir.dstu3.model.Practitioner practitioner = APITestHelpers.createPractitionerResource(NPIUtil.generateNPI(), orgId);
        final MethodOutcome methodOutcome = setupClient.create()
                .resource(practitioner)
                .encodedJson()
                .execute();
        return (org.hl7.fhir.dstu3.model.Practitioner) methodOutcome.getResource();
    }

    private Group createAndSubmitGroup(String orgId, org.hl7.fhir.dstu3.model.Practitioner practitioner, IGenericClient v3Client, List<String> patientIds) {
        final String practitionerId = practitioner.getId();
        final String practitionerNPI = practitioner.getIdentifierFirstRep().getValue();

        final Group group = FHIRGroupBuilder.newBuild()
                .attributedTo(practitionerNPI)
                .withOrgTag(UUID.fromString(orgId))
                .withPatients(patientIds.toArray(String[]::new))
                .build();

        final Provenance provenance = V3APITestHelpers.createProvenance(orgId, practitionerId, Collections.emptyList());
        final MethodOutcome outcome = v3Client.create()
                .resource(group)
                .encodedJson()
                .withAdditionalHeader("X-Provenance", r4Ctx.newJsonParser().encodeResourceToString(provenance))
                .execute();
        return (Group) outcome.getResource();
    }
}
