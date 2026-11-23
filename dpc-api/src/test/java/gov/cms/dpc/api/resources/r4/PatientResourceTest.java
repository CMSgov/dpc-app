package gov.cms.dpc.api.resources.r4;

import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.TestOrganizationContext;
import gov.cms.dpc.api.r4.APITestHelpers;
import gov.cms.dpc.api.r4.AbstractSecureApplicationTest;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.DPCResourceType;
import gov.cms.dpc.fhir.helpers.r4.FHIRHelpers;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.factories.r4.BundleFactory;
import jakarta.ws.rs.HttpMethod;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.ParseException;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static gov.cms.dpc.api.r4.APITestHelpers.ORGANIZATION_ID;
import static gov.cms.dpc.api.r4.APITestHelpers.ORGANIZATION_NPI;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.Random.class)
class PatientResourceTest extends AbstractSecureApplicationTest {
    private static final Logger logger = LoggerFactory.getLogger(PatientResourceTest.class);

    final IParser parser = ctx.newJsonParser();
    final IGenericClient attrClient = APITestHelpers.buildAttributionClient(ctx);

    @BeforeEach
    void resetApplication() throws Exception {
        setup();
    }

    @Test
    void testCreatePatientReturnsAppropriateHeaders() {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        Patient patient = APITestHelpers.createPatientResource("4S41C00AA00", ORGANIZATION_ID);

        MethodOutcome methodOutcome = client.create()
                .resource(patient)
                .encodedJson()
                .execute();

        String location = methodOutcome.getResponseHeaders().get("location").get(0);
        String date = methodOutcome.getResponseHeaders().get("last-modified").get(0);
        assertNotNull(location);
        assertNotNull(date);
        assertTrue(location.contains("/v3/Patient/"));

        Patient foundPatient = client.read()
                .resource(Patient.class)
                .withUrl(location)
                .encodedJson()
                .execute();

        assertEquals(patient.getIdentifierFirstRep().getValue(), foundPatient.getIdentifierFirstRep().getValue());
    }

    @Test
    void ensurePatientsExist() throws IOException, URISyntaxException, GeneralSecurityException, ParseException {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);

        final Bundle patients = fetchPatients(client);
        assertEquals(101, patients.getTotal(), "Should have correct number of patients");

        final Bundle specificSearch = fetchPatientBundleByMBI(client, "4S41C00AA00");
        assertEquals(1, specificSearch.getTotal(), "Should have a single patient");

        final Patient foundPatient = (Patient) specificSearch.getEntryFirstRep().getResource();
        final Patient queriedPatient = client
                .read()
                .resource(Patient.class)
                .withId(foundPatient.getIdElement())
                .encodedJson()
                .execute();
        assertTrue(foundPatient.equalsDeep(queriedPatient), "Search and GET should be identical");

        final String m2 = FHIRHelpers.registerOrganization(attrClient, parser, OTHER_ORG_ID, "1112111111", getAdminURL());
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey("new-key", OTHER_ORG_ID, GOLDEN_MACAROON, getBaseURL());
        client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), m2, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());

        final Bundle otherPatients = fetchPatients(client);
        assertEquals(0, otherPatients.getTotal(), "Should not have any patients");

        final IReadExecutable<Patient> fetchRequest = client
                .read()
                .resource(Patient.class)
                .withId(foundPatient.getId())
                .encodedJson();
        assertThrows(ResourceNotFoundException.class, fetchRequest::execute, "Should not be authorized");

        final Bundle otherSpecificSearch = client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().identifier(foundPatient.getIdentifierFirstRep().getValue()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
        assertEquals(0, otherSpecificSearch.getTotal(), "Should not have a specific patient");
    }

    @Test
    void testPatientRemoval() throws IOException, URISyntaxException, GeneralSecurityException, ParseException {
        final String macaroon = FHIRHelpers.registerOrganization(attrClient, parser, ORGANIZATION_ID, ORGANIZATION_NPI, getAdminURL());
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey("patient-deletion-key", ORGANIZATION_ID, GOLDEN_MACAROON, getBaseURL());
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());
        APITestHelpers.setupPatientTest(client, parser);

        final Bundle patients = fetchPatients(client);
        assertEquals(101, patients.getTotal(), "Should have correct number of patients");

        final Patient patient = (Patient) patients.getEntry().get(patients.getTotal() - 2).getResource();
        client.delete()
                .resource(patient)
                .encodedJson()
                .execute();

        final IReadExecutable<Patient> fetchRequest = client
                .read()
                .resource(Patient.class)
                .withId(patient.getId())
                .encodedJson();
        assertThrows(ResourceNotFoundException.class, fetchRequest::execute, "Should not have found the resource");

        final Bundle secondSearch = fetchPatients(client);
        assertEquals(100, secondSearch.getTotal(), "Should have correct number of patients");
    }

    @Test
    void testPatientUpdating() throws IOException, URISyntaxException, GeneralSecurityException, ParseException {
        final String macaroon = FHIRHelpers.registerOrganization(attrClient, parser, ORGANIZATION_ID, ORGANIZATION_NPI, getAdminURL());
        final Pair<UUID, PrivateKey> uuidPrivateKeyPair = APIAuthHelpers.generateAndUploadKey("patient-update-key", ORGANIZATION_ID, GOLDEN_MACAROON, getBaseURL());
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), macaroon, uuidPrivateKeyPair.getLeft(), uuidPrivateKeyPair.getRight());
        APITestHelpers.setupPatientTest(client, parser);

        final Bundle patients = fetchPatients(client);
        assertEquals(101, patients.getTotal(), "Should have correct number of patients");

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

    @Test
    void testGetPatientByUUID() throws GeneralSecurityException, IOException, URISyntaxException, ParseException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        final Patient orgAPatient = (Patient) APITestHelpers.createResource(orgAClient, APITestHelpers.createPatientResource("4S41C00AA00", orgAContext.getOrgId())).getResource();
        final Patient orgBPatient = (Patient) APITestHelpers.createResource(orgBClient, APITestHelpers.createPatientResource("4S41C00AA00", orgBContext.getOrgId())).getResource();

        assertNotNull(APITestHelpers.getResourceById(orgAClient, Patient.class, orgAPatient.getId()), "Org should be able to retrieve their own patient.");
        assertNotNull(APITestHelpers.getResourceById(orgBClient, Patient.class, orgBPatient.getId()), "Org should be able to retrieve their own patient.");
        final String orgBPatientId = orgBPatient.getId();
        assertThrows(ResourceNotFoundException.class, () -> APITestHelpers.getResourceById(orgAClient, Patient.class, orgBPatientId), "Expected auth error when retrieving another org's patient.");
    }

    @Test
    void testFetchPatientRandomPage() throws IOException {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        Map<String, List<String>> params = new HashMap<>();
        params.put("_count", List.of("10"));
        params.put("_offset", List.of("10"));

        final Bundle patientPage = APITestHelpers.resourceSearch(client, DPCResourceType.Patient, params);
        assertEquals(10, patientPage.getEntry().size());
    }

    @Test
    void testFetchPatientSummaryBundle() throws IOException {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        Map<String, List<String>> params = new HashMap<>();
        params.put("_count", List.of("0"));
        final Bundle summaryBundle = APITestHelpers.resourceSearch(client, DPCResourceType.Patient, params);
        assertTrue(summaryBundle.getEntry().isEmpty());
        assertEquals(101, summaryBundle.getTotal());
    }

    @Test
    void testFetchPatientFollowLinksHappyPath() throws IOException {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        Map<String, List<String>> params = new HashMap<>();
        params.put("_count", List.of("50"));
        final Bundle firstPage = APITestHelpers.resourceSearch(client, DPCResourceType.Patient, params);

        String expectedSecondLinkUrl = getBaseURL() + "/Patient?_count=50&_offset=50";
        String secondLink = firstPage.getLink(Bundle.LINK_NEXT).getUrl();
        assertEquals(expectedSecondLinkUrl, secondLink);
        Bundle secondPage = client.loadPage().next(firstPage).execute();

        String expectedThirdLinkUrl = getBaseURL() + "/Patient?_count=50&_offset=100";
        String thirdLink = secondPage.getLink(Bundle.LINK_NEXT).getUrl();
        assertEquals(expectedThirdLinkUrl, thirdLink);
        Bundle thirdPage = client.loadPage().next(secondPage).execute();

        assertEquals(1, thirdPage.getEntry().size(), "should only contain the 101st patient");
    }

    @Test
    void testFetchPatientInvalidParams() throws IOException {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        Map<String, List<String>> invalidCountParams = new HashMap<>();
        invalidCountParams.put("_count", List.of("-1"));
        Map<String, List<String>> invalidOffsetParams = new HashMap<>();
        invalidOffsetParams.put("_offset", List.of("-200"));

        assertThrows(InvalidRequestException.class, () -> APITestHelpers.resourceSearch(client, DPCResourceType.Patient, invalidCountParams));
        assertThrows(InvalidRequestException.class, () -> APITestHelpers.resourceSearch(client, DPCResourceType.Patient, invalidOffsetParams));
    }

    @Test
    void testFetchPatientHighCountNumber() throws IOException {
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        APITestHelpers.setupPatientTest(client, parser);
        Map<String, List<String>> params = new HashMap<>();
        params.put("_count", List.of("101"));
        Bundle countMatchingPatientNumBundle = APITestHelpers.resourceSearch(client, DPCResourceType.Patient, params);
        assertEquals(101, countMatchingPatientNumBundle.getEntry().size());

        Map<String, List<String>> bigCountParams = new HashMap<>();
        bigCountParams.put("_count", List.of("9001"));
        Bundle patientBundleFromBigCountParam = APITestHelpers.resourceSearch(client, DPCResourceType.Patient, bigCountParams);
        assertEquals(101, patientBundleFromBigCountParam.getEntry().size());
    }

    @Test
    void testUpdatePatientDataDuringPagination() {
        final int FIRST_PATIENT_COUNT = 95;
        final int ADDITIONAL_PATIENT_COUNT = 10;
        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);

        submitPatients(client, ORGANIZATION_ID, FIRST_PATIENT_COUNT);

        Map<String, List<String>> params = new HashMap<>();
        params.put("_count", List.of("50"));
        Bundle firstPage = APITestHelpers.resourceSearch(client, DPCResourceType.Patient, params);
        Bundle secondPagePartialPage = client.loadPage().next(firstPage).execute();
        assertNull(secondPagePartialPage.getLink(IBaseBundle.LINK_NEXT));
        assertEquals(45, secondPagePartialPage.getEntry().size());

        submitPatients(client, ORGANIZATION_ID, ADDITIONAL_PATIENT_COUNT);

        Bundle secondPageFullPage = client.loadPage().next(firstPage).execute();
        assertNotNull(secondPageFullPage.getLink(IBaseBundle.LINK_NEXT));
        assertEquals(50, secondPageFullPage.getEntry().size());
        Bundle thirdPage = client.loadPage().next(secondPageFullPage).execute();
        assertEquals(5, thirdPage.getEntry().size());
    }

    @Test
    void testDeletePatient() throws GeneralSecurityException, IOException, URISyntaxException, ParseException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        final Patient orgBPatient = (Patient) APITestHelpers.createResource(orgBClient, APITestHelpers.createPatientResource("4S41C00AA00", orgBContext.getOrgId())).getResource();
        final String orgBPatientId = orgBPatient.getIdElement().getIdPart();

        assertThrows(ResourceNotFoundException.class, () -> APITestHelpers.deleteResourceById(orgAClient, DPCResourceType.Patient, orgBPatientId), "Expected auth error when deleting another org's patient.");
        APITestHelpers.deleteResourceById(orgBClient, DPCResourceType.Patient, orgBPatientId);
    }

    @Test
    void testPatientPathAuthorization() throws GeneralSecurityException, IOException, URISyntaxException, ParseException {
        final TestOrganizationContext orgAContext = registerAndSetupNewOrg();
        final TestOrganizationContext orgBContext = registerAndSetupNewOrg();
        final IGenericClient orgAClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgAContext.getClientToken(), UUID.fromString(orgAContext.getPublicKeyId()), orgAContext.getPrivateKey());
        final IGenericClient orgBClient = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), orgBContext.getClientToken(), UUID.fromString(orgBContext.getPublicKeyId()), orgBContext.getPrivateKey());

        final Patient orgAPatient = (Patient) APITestHelpers.createResource(orgAClient, APITestHelpers.createPatientResource(MockBlueButtonClient.TEST_PATIENT_MBIS.get(2), orgAContext.getOrgId())).getResource();
        final Patient orgBPatient = (Patient) APITestHelpers.createResource(orgBClient, APITestHelpers.createPatientResource("4S41C00AA00", orgBContext.getOrgId())).getResource();
        final String orgAPatientId = orgAPatient.getIdElement().getIdPart();
        final String orgBPatientId = orgBPatient.getIdElement().getIdPart();

        APITestHelpers.getResourceById(orgAClient, Patient.class, orgAPatientId);
        APITestHelpers.getResourceById(orgBClient, Patient.class, orgBPatientId);
        assertThrows(ResourceNotFoundException.class, () -> APITestHelpers.getResourceById(orgBClient, Patient.class, orgAPatientId), "Expected auth error when accessing another org's patient");

        APITestHelpers.updateResource(orgAClient, orgAPatientId, orgAPatient);
        APITestHelpers.updateResource(orgBClient, orgBPatientId, orgBPatient);
        assertThrows(ResourceNotFoundException.class, () -> APITestHelpers.updateResource(orgBClient, orgAPatientId, orgAPatient), "Expected auth error when updating another org's patient");

        APITestHelpers.updateResource(orgAClient, orgAPatientId, orgAPatient);
        APITestHelpers.updateResource(orgBClient, orgBPatientId, orgBPatient);
        assertThrows(ResourceNotFoundException.class, () -> APITestHelpers.updateResource(orgBClient, orgAPatientId, orgAPatient), "Expected auth error when updating another org's patient");
    }

    @Test
    void testBatchSubmit() throws GeneralSecurityException, IOException, URISyntaxException, ParseException {
        final int COUNT_TEST_PATIENTS = 500;

        IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        final TestOrganizationContext orgContext = registerAndSetupNewOrg();
        List<Patient> patients = APITestHelpers.createPatientResources(orgContext.getOrgId(), COUNT_TEST_PATIENTS);

        Bundle patientBundle = BundleFactory.createBundle(
                patients.stream().map(Resource.class::cast).toArray(Resource[]::new)
        );
        Parameters params = new Parameters();
        params.addParameter().setResource(patientBundle);

        Instant startInstant = Instant.now();
        Bundle resultPatientBundle = client
                .operation()
                .onType(Patient.class)
                .named("submit")
                .withParameters(params)
                .returnResourceType(Bundle.class)
                .encodedJson()
                .execute();

        Instant stopInstant = Instant.now();
        Duration submitDuration = Duration.between(startInstant, stopInstant);
        float submitSeconds = submitDuration.getSeconds() + ((float) submitDuration.toMillisPart() / 1000);
        logger.info("Submit operation time: {}", submitSeconds);

        assertEquals(COUNT_TEST_PATIENTS, resultPatientBundle.getEntry().size());
    }

    private Bundle fetchPatients(IGenericClient client) {
        return APITestHelpers.resourceSearch(client, DPCResourceType.Patient);
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

    private Bundle submitPatients(IGenericClient client, String orgId, int numPatients) {
        List<Patient> patients = APITestHelpers.createPatientResources(orgId, numPatients);

        Bundle patientBundle = BundleFactory.createBundle(
                patients.stream().map(Resource.class::cast).toArray(Resource[]::new)
        );
        Parameters params = new Parameters();
        params.addParameter().setResource(patientBundle);

        return client
                .operation()
                .onType(Patient.class)
                .named("submit")
                .withParameters(params)
                .returnResourceType(Bundle.class)
                .encodedJson()
                .execute();
    }
}
