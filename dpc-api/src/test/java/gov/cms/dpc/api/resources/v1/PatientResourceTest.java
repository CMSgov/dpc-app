package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.aggregation.service.ConsentResult;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.api.TestOrganizationContext;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.helpers.FHIRHelpers;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.factories.FHIRPractitionerBuilder;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.*;

import javax.ws.rs.HttpMethod;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.sql.Date;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_ID;
import static gov.cms.dpc.api.APITestHelpers.ORGANIZATION_NPI;
import static org.junit.jupiter.api.Assertions.*;

/*
    If you're running this locally, you'll need to wipe out the rows in the consent table in between runs.  If not, the
    opt outs from the previous run will interfere with the current one.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PatientResourceTest extends AbstractSecureApplicationTest {
    final java.util.Date dateYesterday = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
    final java.util.Date dateToday = Date.from(Instant.now());

    public static final String PROVENANCE_FMT = "{ \"resourceType\": \"Provenance\", \"recorded\": \"" + DateTimeType.now().getValueAsString() + "\"," +
            " \"reason\": [ { \"system\": \"http://hl7.org/fhir/v3/ActReason\", \"code\": \"TREAT\"  } ], \"agent\": [ { \"role\": " +
            "[ { \"coding\": [ { \"system\":" + "\"http://hl7.org/fhir/v3/RoleClass\", \"code\": \"AGNT\" } ] } ], \"whoReference\": " +
            "{ \"reference\": \"Organization/ORGANIZATION_ID\" }, \"onBehalfOfReference\": { \"reference\": " +
            "\"Practitioner/PRACTITIONER_ID\" } } ] }";

    final IParser parser = ctx.newJsonParser();
    final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);
    final IGenericClient consentClient = APITestHelpers.buildConsentClient(ctx);

    @Test
    @Order(1)
    public void testCreatePatientReturnsAppropriateHeaders() {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        Patient patient = APITestHelpers.createPatientResource("4S41C00AA00", APITestHelpers.ORGANIZATION_ID);

        MethodOutcome methodOutcome = client.create()
                .resource(patient)
                .encodedJson()
                .execute();

        String location = methodOutcome.getResponseHeaders().get("location").get(0);
        String date = methodOutcome.getResponseHeaders().get("last-modified").get(0);
        assertNotNull(location);
        assertNotNull(date);

        Patient foundPatient = client.read()
                .resource(Patient.class)
                .withUrl(location)
                .encodedJson()
                .execute();

        assertEquals(patient.getIdentifierFirstRep().getValue(), foundPatient.getIdentifierFirstRep().getValue());

        client.delete()
                .resource(foundPatient)
                .encodedJson()
                .execute();
    }

    @Test
    @Order(2)
    void ensurePatientsExist() throws IOException, URISyntaxException, GeneralSecurityException {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);

        final Bundle patients = fetchPatients(client);
        assertEquals(101, patients.getTotal(), "Should have correct number of patients");

        final Bundle specificSearch = fetchPatientBundleByMBI(client, "4S41C00AA00");

        assertEquals(1, specificSearch.getTotal(), "Should have a single patient");

        // Fetch the patient directly
        final Patient foundPatient = (Patient) specificSearch.getEntryFirstRep().getResource();

        final Patient queriedPatient = client
                .read()
                .resource(Patient.class)
                .withId(foundPatient.getIdElement())
                .encodedJson()
                .execute();

        assertTrue(foundPatient.equalsDeep(queriedPatient), "Search and GET should be identical");

        // Create a new org and make sure it has no providers
        final String m2 = FHIRHelpers.registerOrganization(attrClient, parser, OTHER_ORG_ID, "1112111111", getAdminURL());
        // Submit a new public key to use for JWT flow
        final String keyID = "new-key";
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey(keyID, OTHER_ORG_ID, GOLDEN_MACAROON, getBaseURL());

        // Update the authenticated client to use the new organization
        client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), m2, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());

        final Bundle otherPatients = fetchPatients(client);

        assertEquals(0, otherPatients.getTotal(), "Should not have any patients");

        // Try to look for one of the other patients
        final IReadExecutable<Patient> fetchRequest = client
                .read()
                .resource(Patient.class)
                .withId(foundPatient.getId())
                .encodedJson();

        assertThrows(AuthenticationException.class, fetchRequest::execute, "Should not be authorized");

        // Search, and find nothing
        final Bundle otherSpecificSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().identifier(foundPatient.getIdentifierFirstRep().getValue()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(0, otherSpecificSearch.getTotal(), "Should have a specific provider");
    }

    @Test
    @Order(3)
    void testPatientRemoval() throws IOException, URISyntaxException, GeneralSecurityException {
        final String macaroon = FHIRHelpers.registerOrganization(attrClient, parser, ORGANIZATION_ID, ORGANIZATION_NPI, getAdminURL());
        final String keyLabel = "patient-deletion-key";
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey(keyLabel, ORGANIZATION_ID, GOLDEN_MACAROON, getBaseURL());
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());

        final Bundle patients = fetchPatients(client);

        assertEquals(101, patients.getTotal(), "Should have correct number of patients");

        // Try to remove one

        final Patient patient = (Patient) patients.getEntry().get(patients.getTotal() - 2).getResource();

        client
                .delete()
                .resource(patient)
                .encodedJson()
                .execute();

        // Make sure it's done

        final IReadExecutable<Patient> fetchRequest = client
                .read()
                .resource(Patient.class)
                .withId(patient.getId())
                .encodedJson();

        // TODO: DPC-433, this really should be NotFound, but we can't disambiguate between the two cases
        assertThrows(AuthenticationException.class, fetchRequest::execute, "Should not have found the resource");

        // Search again
        final Bundle secondSearch = fetchPatients(client);

        assertEquals(100, secondSearch.getTotal(), "Should have correct number of patients");
    }

    @Test
    @Order(4)
    void testPatientUpdating() throws IOException, URISyntaxException, GeneralSecurityException {
        final String macaroon = FHIRHelpers.registerOrganization(attrClient, parser, ORGANIZATION_ID, ORGANIZATION_NPI, getAdminURL());
        final String keyLabel = "patient-update-key";
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey(keyLabel, ORGANIZATION_ID, GOLDEN_MACAROON, getBaseURL());
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());

        final Bundle patients = fetchPatients(client);

        assertEquals(100, patients.getTotal(), "Should have correct number of patients");

        // Try to update one
        final Patient patient = (Patient) patients.getEntry().get(patients.getTotal() - 2).getResource();
        patient.setBirthDate(Date.valueOf("2000-01-01"));
        patient.setGender(Enumerations.AdministrativeGender.MALE);

        final MethodOutcome outcome = client
                .update()
                .resource(patient)
                .withId(patient.getId())
                .encodedJson()
                .execute();

        assertTrue(((Patient) outcome.getResource()).equalsDeep(patient), "Should have been updated correctly");

        // Try to update with invalid MBI
        Identifier mbiIdentifier = patient.getIdentifier().stream()
                .filter(i -> DPCIdentifierSystem.MBI.getSystem().equals(i.getSystem())).findFirst().orElseThrow();
        mbiIdentifier.setValue("not-a-valid-MBI");

        IUpdateExecutable update = client
                .update()
                .resource(patient)
                .withId(patient.getId());

        assertThrows(UnprocessableEntityException.class, update::execute);
    }

    @Test
    @Order(5)
    void testCreateInvalidPatient() throws IOException, URISyntaxException {
        URL url = new URL(getBaseURL() + "/Patient");
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
            String respLine;
            while ((respLine = reader.readLine()) != null) {
                respBuilder.append(respLine.trim());
            }
            String resp = respBuilder.toString();
            assertTrue(resp.contains("\"resourceType\":\"OperationOutcome\""));
            assertTrue(resp.contains("Invalid JSON content"));
        }

        conn.disconnect();
    }

    // TODO: dropwizard - debug
    @Disabled
    @Test
    @Order(6)
    void testPatientEverythingWithoutGroupFetchesData() throws IOException, URISyntaxException, GeneralSecurityException {
        IGenericClient client = generateClient(ORGANIZATION_NPI, RandomStringUtils.randomAlphabetic(25));
        APITestHelpers.setupPractitionerTest(client, parser);

        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(2);
        Patient patient = fetchPatient(client, mbi);
        Practitioner practitioner = fetchPractitionerByNPI(client);
        final String patientId = FHIRExtractors.getEntityUUID(patient.getId()).toString();

        // Patient without Group should still return data
        Bundle resultNoSince = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute();

        assertEquals(64, resultNoSince.getTotal(), "Should have 64 entries in Bundle");

        // Request with a blank since parameter should still return data
        Bundle resultEmptySince = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withSearchParameter(Parameters.class, "_since", new StringParam(""))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute();

        assertEquals(64, resultEmptySince.getTotal(), "Should have 64 entries in Bundle");

        // Request with an invalid since parameter should throw an error
        assertThrows(InvalidRequestException.class, () -> client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withSearchParameter(Parameters.class, "_since", new StringParam("foo"))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute());

        // Request with a since parameter in the future should throw an error
        String sinceInvalid = OffsetDateTime.now(ZoneId.of("America/Puerto_Rico")).plusDays(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertThrows(InvalidRequestException.class, () -> client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withSearchParameter(Parameters.class, "_since", new StringParam(sinceInvalid))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute());

        // Request with a valid since parameter should return data
        String sinceValid = OffsetDateTime.now(ZoneId.of("America/Puerto_Rico")).minusSeconds(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Bundle resultValidSince = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withSearchParameter(Parameters.class, "_since", new StringParam(sinceValid))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute();

        assertEquals(0, resultValidSince.getTotal(), "Should have 0 entries in Bundle");
    }

    // TODO: dropwizard - debug
    @Disabled
    @Test
    @Order(7)
    void testPatientEverythingWithGroupFetchesData() throws IOException, URISyntaxException, GeneralSecurityException {
        IGenericClient client = generateClient(ORGANIZATION_NPI, RandomStringUtils.randomAlphabetic(25));

        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(2);
        Patient patient = fetchPatient(client, mbi);
        Practitioner practitioner = fetchPractitionerByNPI(client);
        final String patientId = FHIRExtractors.getEntityUUID(patient.getId()).toString();

        // Patient in Group should also return data
        Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), ORGANIZATION_ID);
        Reference patientRef = new Reference("Patient/" + patientId);
        group.addMember().setEntity(patientRef);

        client
                .create()
                .resource(group)
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .encodedJson()
                .execute();

        Bundle resultNoSince = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute();

        assertEquals(64, resultNoSince.getTotal(), "Should have 64 entries in Bundle");
        for (Bundle.BundleEntryComponent bec : resultNoSince.getEntry()) {
            List<DPCResourceType> resourceTypes = List.of(DPCResourceType.Coverage, DPCResourceType.ExplanationOfBenefit, DPCResourceType.Patient);
            assertTrue(resourceTypes.stream().map(Enum::toString).collect(Collectors.toList()).contains(bec.getResource().getResourceType().toString()), "Resource type should be Coverage, EOB, or Patient");
        }

        Bundle resultEmptySince = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withSearchParameter(Parameters.class, "_since", new StringParam(""))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute();

        assertEquals(64, resultEmptySince.getTotal(), "Should have 64 entries in Bundle");

        assertThrows(InvalidRequestException.class, () -> client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withSearchParameter(Parameters.class, "_since", new StringParam("foo"))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute());

        String sinceInvalid = OffsetDateTime.now(ZoneId.of("America/Puerto_Rico")).plusDays(10).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertThrows(InvalidRequestException.class, () -> client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withSearchParameter(Parameters.class, "_since", new StringParam(sinceInvalid))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute());

        // Request with a valid since parameter should return data
        String sinceValid = OffsetDateTime.now(ZoneId.of("America/Puerto_Rico")).minusSeconds(5).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Bundle resultValidSince = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withSearchParameter(Parameters.class, "_since", new StringParam(sinceValid))
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute();

        assertEquals(0, resultValidSince.getTotal(), "Should have 0 entries in Bundle");
    }

    // TODO: dropwizard - debug
    @Disabled
    @Test
    @Order(8)
    void testPatientEverything_CanHandlePatientWithMultipleMBIs() throws IOException, URISyntaxException, GeneralSecurityException {
        IGenericClient client = generateClient(ORGANIZATION_NPI, RandomStringUtils.randomAlphabetic(25));

        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(6);
        Patient patient = fetchPatient(client, mbi);
        Practitioner practitioner = fetchPractitionerByNPI(client);
        final String patientId = FHIRExtractors.getEntityUUID(patient.getId()).toString();

        Bundle everythingBundle = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute();

        Patient patientResource = (Patient) everythingBundle.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType().getPath() == "patient")
                .findFirst().get().getResource();

        // Patient should have multiple MBIs
        List<String> mbis =  FHIRExtractors.getPatientMBIs(patientResource);
        assertEquals(2, mbis.size());
        assertTrue(mbis.containsAll(List.of("9V99EU8XY91", "1S00EU8FE91")));

        // Current MBI
        assertEquals("9V99EU8XY91", FHIRExtractors.getPatientMBI(patientResource));
    }

    // TODO: dropwizard - debug
    @Disabled
    @Test
    @Order(9)
    void testPatientEverythingForOptedOutPatient() throws IOException, URISyntaxException, GeneralSecurityException {
        IGenericClient client = generateClient(ORGANIZATION_NPI, RandomStringUtils.randomAlphabetic(25));

        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(2);
        Patient patient = fetchPatient(client, mbi);
        Practitioner practitioner = fetchPractitionerByNPI(client);
        final String patientId = FHIRExtractors.getEntityUUID(patient.getId()).toString();

        optOutPatient(mbi, dateYesterday);

        IOperationUntypedWithInput<Bundle> getEverythingOperation = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()));

        InternalErrorException exception = assertThrows(InternalErrorException.class, getEverythingOperation::execute, "Expected Internal server error when retrieving opted out patient.");
        assertTrue(exception.getResponseBody().contains("\"text\":\"Data not available for opted out patient\""), "Incorrect or missing operation outcome in response body.");
    }

    // TODO: dropwizard - debug
    @Disabled
    @Test
    @Order(10)
    void testPatientEverythingForOptedOutPatientOnMultipleMbis() throws IOException, URISyntaxException, GeneralSecurityException {
        IGenericClient client = generateClient(ORGANIZATION_NPI, RandomStringUtils.randomAlphabetic(25));

        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(6);
        String historicMbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(7);

        Patient patient = fetchPatient(client, mbi);
        Practitioner practitioner = fetchPractitionerByNPI(client);
        final String patientId = FHIRExtractors.getEntityUUID(patient.getId()).toString();

        optOutPatient(historicMbi, dateYesterday);

        IOperationUntypedWithInput<Bundle> getEverythingOperation = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()));

        InternalErrorException exception = assertThrows(InternalErrorException.class, getEverythingOperation::execute, "Expected Internal server error when retrieving opted out patient.");
        assertTrue(exception.getResponseBody().contains("\"text\":\"Data not available for opted out patient\""), "Incorrect or missing operation outcome in response body.");
    }

    // TODO: dropwizard - debug
    @Disabled
    @Test
    public void testOptInPatient() throws GeneralSecurityException, IOException, URISyntaxException {
        IGenericClient client = generateClient(ORGANIZATION_NPI, RandomStringUtils.randomAlphabetic(25));

        String mbi = MockBlueButtonClient.TEST_PATIENT_MBIS.get(2);
        Patient patient = fetchPatient(client, mbi);
        Practitioner practitioner = fetchPractitionerByNPI(client);
        final String patientId = FHIRExtractors.getEntityUUID(patient.getId()).toString();

        optInPatient(mbi, dateToday);

        Bundle bundle = client
                .operation()
                .onInstance(new IdType("Patient", patientId))
                .named("$everything")
                .withNoParameters(Parameters.class)
                .returnResourceType(Bundle.class)
                .useHttpGet()
                .withAdditionalHeader("X-Provenance", generateProvenance(ORGANIZATION_ID, practitioner.getId()))
                .execute();

        Patient patientResource = (Patient) bundle.getEntry().stream()
                .filter(entry -> entry.getResource().getResourceType().getPath() == "patient")
                .findFirst().get().getResource();

        // Patient should have multiple MBIs
        String resultMbi =  FHIRExtractors.getPatientMBI(patientResource);
        assertEquals(MockBlueButtonClient.TEST_PATIENT_MBIS.get(2), resultMbi );
    }

    @Test
    public void testGetPatientByUUID() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        //Setup org A with a patient
        final Patient orgAPatient = (Patient) APITestHelpers.createResource(orgAClient, APITestHelpers.createPatientResource("4S41C00AA00", orgAContext.getOrgId())).getResource();

        //Setup org B with a patient
        final Patient orgBPatient = (Patient) APITestHelpers.createResource(orgBClient, APITestHelpers.createPatientResource("4S41C00AA00", orgBContext.getOrgId())).getResource();

        assertNotNull(APITestHelpers.getResourceById(orgAClient, Patient.class,orgAPatient.getId()), "Org should be able to retrieve their own patient.");
        assertNotNull(APITestHelpers.getResourceById(orgBClient,Patient.class, orgBPatient.getId()), "Org should be able to retrieve their own patient.");
        assertThrows(AuthenticationException.class, () -> APITestHelpers.getResourceById(orgAClient,Patient.class, orgBPatient.getId()), "Expected auth error when retrieving another org's patient.");
    }

    @Test
    public void testDeletePatient() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        //Setup org B with a patient
        final Patient orgBPatient = (Patient) APITestHelpers.createResource(orgBClient, APITestHelpers.createPatientResource("4S41C00AA00", orgBContext.getOrgId())).getResource();

        assertThrows(AuthenticationException.class, () -> APITestHelpers.deleteResourceById(orgAClient, DPCResourceType.Patient, orgBPatient.getIdElement().getIdPart()), "Expected auth error when deleting another org's patient.");
        APITestHelpers.deleteResourceById(orgBClient, DPCResourceType.Patient, orgBPatient.getIdElement().getIdPart());
    }

    // TODO: dropwizard - debug
    @Disabled
    @Test
    public void testPatientPathAuthorization() throws GeneralSecurityException, IOException, URISyntaxException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        final Patient orgAPatient = (Patient) APITestHelpers.createResource(orgAClient, APITestHelpers.createPatientResource(MockBlueButtonClient.TEST_PATIENT_MBIS.get(2), orgAContext.getOrgId())).getResource();
        final Patient orgBPatient = (Patient) APITestHelpers.createResource(orgBClient, APITestHelpers.createPatientResource("4S41C00AA00", orgBContext.getOrgId())).getResource();

        //Test GET /Patient/{id}
        APITestHelpers.getResourceById(orgAClient,Patient.class,orgAPatient.getIdElement().getIdPart());
        APITestHelpers.getResourceById(orgBClient,Patient.class,orgBPatient.getIdElement().getIdPart());
        assertThrows(AuthenticationException.class, () -> APITestHelpers.getResourceById(orgBClient,Patient.class,orgAPatient.getIdElement().getIdPart()), "Expected auth error when accessing another org's patient");

        //Test PUT /Patient/{id}
        APITestHelpers.updateResource(orgAClient,orgAPatient.getIdElement().getIdPart(),orgAPatient);
        APITestHelpers.updateResource(orgBClient,orgBPatient.getIdElement().getIdPart(),orgBPatient);
        assertThrows(AuthenticationException.class, () -> APITestHelpers.updateResource(orgBClient,orgAPatient.getIdElement().getIdPart(),orgAPatient), "Expected auth error when updating another org's patient");

        //Test PUT /Patient/{id}
        APITestHelpers.updateResource(orgAClient,orgAPatient.getIdElement().getIdPart(),orgAPatient);
        APITestHelpers.updateResource(orgBClient,orgBPatient.getIdElement().getIdPart(),orgBPatient);
        assertThrows(AuthenticationException.class, () -> APITestHelpers.updateResource(orgBClient,orgAPatient.getIdElement().getIdPart(),orgAPatient), "Expected auth error when updating another org's patient");


        //Test Get /Patient/{id}/$everything
        Practitioner orgAPractitioner = FHIRPractitionerBuilder.newBuilder()
                .withOrgTag(orgAContext.getOrgId())
                .withNpi("1234329724")
                .withName("Org A Practitioner", "Last name")
                .build();
        orgAPractitioner = (Practitioner) APITestHelpers.createResource(orgAClient, orgAPractitioner).getResource();

       Bundle result = APITestHelpers.getPatientEverything(orgAClient, orgAPatient.getIdElement().getIdPart(), generateProvenance(orgAContext.getOrgId(),orgAPractitioner.getIdElement().getIdPart()));
       assertEquals(64, result.getTotal(), "Should have 64 entries in Bundle");

        final String orgAPractitionerId = orgAPractitioner.getIdElement().getIdPart();
       assertThrows(AuthenticationException.class, () ->
               APITestHelpers.getPatientEverything(orgBClient, orgAPatient.getIdElement().getIdPart(), generateProvenance(orgAContext.getOrgId(), orgAPractitionerId))
       , "Expected auth error when export another org's patient's data");
    }

    private IGenericClient generateClient(String orgNPI, String keyLabel) throws IOException, URISyntaxException, GeneralSecurityException {
        final String macaroon = FHIRHelpers.registerOrganization(attrClient, parser, APITestHelpers.ORGANIZATION_ID, orgNPI, getAdminURL());
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey(keyLabel, APITestHelpers.ORGANIZATION_ID, GOLDEN_MACAROON, getBaseURL());
        return APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight(), false, true);
    }

    private String generateProvenance(String orgID, String practitionerID) {
        return PROVENANCE_FMT.replaceAll("ORGANIZATION_ID", orgID).replace("PRACTITIONER_ID", practitionerID);
    }

    private Bundle fetchPatients(IGenericClient client) {
        return APITestHelpers.resourceSearch(client,DPCResourceType.Patient);
    }

    private Patient fetchPatient(IGenericClient client, String mbi) {
        return (Patient) fetchPatientBundleByMBI(client, mbi).getEntry().get(0).getResource();
    }

    private Bundle fetchPatientBundleByMBI(IGenericClient client, String mbi) {
        return client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), mbi))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
    }

    private Practitioner fetchPractitionerByNPI(IGenericClient client) {
        Bundle practSearch = client
                .search()
                .forResource(Practitioner.class)
                .where(Practitioner.IDENTIFIER.exactly().code("1234329724"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        return (Practitioner) practSearch.getEntry().get(0).getResource();
    }

    private void optOutPatient(String mbi, java.util.Date date){
        sendConsent(mbi, ConsentResult.PolicyType.OPT_OUT.getValue(), date);
    }

    private void optInPatient(String mbi, java.util.Date date){
        sendConsent(mbi, ConsentResult.PolicyType.OPT_IN.getValue(), date);
    }

    private void sendConsent(String mbi, String policyUrl, java.util.Date date) {
        Consent consent = new Consent();
        consent.setStatus(Consent.ConsentState.ACTIVE);

        Coding categoryCoding = new Coding("http://loinc.org","64292-6", null);
        CodeableConcept category = new CodeableConcept();
        category.setCoding(List.of(categoryCoding));
        consent.setCategory(List.of(category));

        String patientRefPath = "/Patient?identity=|"+mbi;
        consent.setPatient(new Reference("http://api.url" + patientRefPath));

        consent.setDateTime(date);

        Reference orgRef = new Reference("Organization/" + UUID.randomUUID().toString());
        consent.setOrganization(List.of(orgRef));

        consent.setPolicyRule(policyUrl);

        consentClient
                .create()
                .resource(consent)
                .encodedJson()
                .execute();
    }
}
