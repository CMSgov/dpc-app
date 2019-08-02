package gov.cms.dpc.attribution;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.NotModifiedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRBuilders;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AttributionFHIRTest {

    private static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAttributionService.class, null, ConfigOverride.config("server.applicationConnectors[0].port", "3727"));
    private static final FhirContext ctx = FhirContext.forDstu3();
    private static final String CSV = "test_associations.csv";
    private static Map<String, List<Pair<String, String>>> groupedPairs = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Organization organization;

    @BeforeAll
    static void setup() throws Exception {
        APPLICATION.before();
        APPLICATION.getApplication().run("db", "drop-all", "--confirm-delete-everything");
        APPLICATION.getApplication().run("db", "migrate");

        // Get the test seeds
        final InputStream resource = AttributionFHIRTest.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", AttributionFHIRTest.class.getName(), CSV);
        }

        // Read in the seeds and create the 'Roster' bundle
        groupedPairs = SeedProcessor.extractProviderMap(resource);

        // Create the Organization
        organization = AttributionTestHelpers.createOrganization(ctx, String.format("http://localhost:%s/v1/", APPLICATION.getLocalPort()));
    }

    @AfterAll
    static void shutdown() {
        APPLICATION.after();
    }

    @TestFactory
    Stream<DynamicTest> generateRosterTests() {

        // Create the Organization

        BiFunction<Bundle, String, String> nameGenerator = (bundle, operation) -> String.format("[%s] provider: %s", operation.toUpperCase(), ((Practitioner) bundle.getEntryFirstRep().getResource()).getIdentifierFirstRep().getValue());

        final UUID orgID = UUID.fromString(organization.getIdElement().getIdPart());

        // Get all the provider IDs and generate tests for them.
        return groupedPairs
                .entrySet()
                .stream()
                .map((Map.Entry<String, List<Pair<String, String>>> entry) -> SeedProcessor.generateRosterBundle(entry, orgID))
                .flatMap((bundle) -> Stream.of(
                        DynamicTest.dynamicTest(nameGenerator.apply(bundle, "Submit"), () -> submitRoster(bundle)),
                        DynamicTest.dynamicTest(nameGenerator.apply(bundle, "Update"), () -> updateRoster(bundle)),
                        DynamicTest.dynamicTest(nameGenerator.apply(bundle, "Remove"), () -> removeRoster(bundle))));
    }

    private void submitRoster(Bundle bundle) {

        // Provider first, then patients
        final Practitioner practitioner = (Practitioner) bundle.getEntryFirstRep().getResource();
        final String providerID = practitioner.getIdentifierFirstRep().getValue();
        final String organizationID = FHIRExtractors.getOrganizationID(practitioner);
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        final MethodOutcome createdPractitioner = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        assertTrue(createdPractitioner.getCreated(), "Should have created the practitioner");


        // Create a group and add Patients to it
        final Group rosterGroup = createBaseAttributionGroup(providerID, organizationID);

        bundle
                .getEntry()
                .stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(resource -> resource.getResourceType() == ResourceType.Patient)
                .map(resource -> (Patient) resource)
                .forEach(patient -> {
                    try {
                        final MethodOutcome created = client
                                .create()
                                .resource(patient)
                                .encodedJson()
                                .execute();
                        final Patient pr = (Patient) created.getResource();
                        // Add to group
                        rosterGroup
                                .addMember()
                                .setEntity(new Reference(pr.getIdElement()))
                                .setInactive(false);

                        assertTrue(created.getCreated(), "Should have created the patient");
                    } catch (NotModifiedException e) {
                        e.getMessage();
                    }
                });

        final ICreateTyped groupCreation = client
                .create()
                .resource(rosterGroup)
                .encodedJson();

        final MethodOutcome groupCreated = groupCreation.execute();

        assertTrue(groupCreated.getCreated(), "Should have created the group");

        final Group createdGroup = (Group) groupCreated.getResource();

        // Get the patients
        final IReadExecutable<Group> groupSizeQuery = client
                .read()
                .resource(Group.class)
                .withId(createdGroup.getId())
                .encodedJson();

        final Group fetchedGroup = groupSizeQuery
                .execute();
        // Remove meta
        fetchedGroup.setMeta(null);

        assertAll(() -> assertTrue(createdGroup.equalsDeep(fetchedGroup), "Groups should be equal"),
                () -> assertEquals(bundle.getEntry().size() - 1, fetchedGroup.getMember().size(), "Should have the same number of beneies"));

        final String patientID = ((Patient) bundle.getEntry().get(1).getResource()).getIdentifierFirstRep().getValue();

        final Bundle searchedPatient = client
                .search()
                .forResource(Group.class)
                .where(Group.MEMBER.hasId(patientID))
                .where(Group.CHARACTERISTIC.exactly().code(providerID))
                .withTag("", organizationID)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, searchedPatient.getTotal(), "Should only have a single group");

        groupCreation.execute();

        final Group group2 = groupSizeQuery.execute();
        assertAll(() -> assertTrue(fetchedGroup.equalsDeep(group2), "Groups should be equal"),
                () -> assertEquals(bundle.getEntry().size() - 1, group2.getMember().size(), "Should have the same number of beneies"));
    }


    private void updateRoster(Bundle bundle) throws IOException {

        final Practitioner practitioner = (Practitioner) bundle.getEntryFirstRep().getResource();
        final String providerID = practitioner.getIdentifierFirstRep().getValue();
        final String organizationID = FHIRExtractors.getOrganizationID(practitioner);

        final HttpGet getPatients = new HttpGet("http://localhost:" + APPLICATION.getLocalPort() + "/v1/Group/" + providerID);
        getPatients.setHeader("Accept", FHIRMediaTypes.FHIR_JSON);

        try (final CloseableHttpClient httpClient = HttpClients.createDefault()) {

            // Create the new patient and submit them
            final Patient patient = new Patient();
            final Identifier patientIdentifier = new Identifier();
            patientIdentifier.setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-new-patient-id");
            patient.addIdentifier(patientIdentifier);
            patient.addName().addGiven("New Test Patient");
            patient.setBirthDate(new GregorianCalendar(2019, Calendar.MARCH, 1).getTime());
            patient.setManagingOrganization(new Reference(new IdType("Organization", organizationID)));

            ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
            final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

            final MethodOutcome patientCreated = client
                    .create()
                    .resource(patient)
                    .encodedJson()
                    .execute();

            assertTrue(patientCreated.getCreated(), "Should be created");

            final Patient newPatient = (Patient) patientCreated.getResource();

            // Find the existing Roster ID
            final Bundle searchedProviderGroup = client
                    .search()
                    .forResource(Group.class)
                    .where(Group.CHARACTERISTIC.exactly().code(providerID))
                    .withTag("", organizationID)
                    .returnBundle(Bundle.class)
                    .encodedJson()
                    .execute();

            assertEquals(1, searchedProviderGroup.getTotal(), "Should have 1 group");
            final String groupID = searchedProviderGroup.getEntryFirstRep().getResource().getId();

            // Create a new Group, and update
            final Group newRoster = createBaseAttributionGroup(providerID, organizationID);
            final Reference patientReference = new Reference(newPatient.getId());
            newRoster.addMember().setEntity(patientReference);

            // Update the roster
            final IUpdateExecutable updateGroupRequest = client
                    .update()
                    .resource(newRoster)
                    .withId(groupID)
                    .encodedJson();

            updateGroupRequest
                    .execute();

            // Check how many are attributed

            final IReadExecutable<Group> getUpdatedGroup = client
                    .read()
                    .resource(Group.class)
                    .withId(groupID)
                    .encodedJson();
            final Group updatedGroup = getUpdatedGroup
                    .execute();

            assertEquals(bundle.getEntry().size(), updatedGroup.getMember().size(), "Should have an additional patient");

            final String patientMPI = FHIRExtractors.getPatientMPI(newPatient);
            final Bundle searchedPatient = client
                    .search()
                    .forResource(Group.class)
                    .where(Group.MEMBER.hasId(patientMPI))
                    .where(Group.CHARACTERISTIC.exactly().code(providerID))
                    .withTag("", organizationID)
                    .returnBundle(Bundle.class)
                    .encodedJson()
                    .execute();

            assertEquals(1, searchedPatient.getTotal(), "Should only have a single group");

            // Remove the patient
            final Group.GroupMemberComponent removeEntity = new Group.GroupMemberComponent().setEntity(patientReference).setInactive(true);
            newRoster
                    .addMember(removeEntity);

            assertThrows(InvalidRequestException.class, updateGroupRequest::execute, "Should have a bad request");
            newRoster.setMember(List.of(removeEntity));
            updateGroupRequest.execute();

            assertEquals(bundle.getEntry().size() - 1,
                    getUpdatedGroup.execute().getMember().size(),
                    "Should have a missing patient");
        }
    }

    private void removeRoster(Bundle bundle) {

        final Practitioner practitioner = (Practitioner) bundle.getEntryFirstRep().getResource();
        final String providerID = practitioner.getIdentifierFirstRep().getValue();
        final String organizationID = FHIRExtractors.getOrganizationID(practitioner);
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        final IQuery<Bundle> bundleSearchRequest = client
                .search()
                .forResource(Group.class)
                .returnBundle(Bundle.class)
                .where(Group.CHARACTERISTIC.exactly().code(providerID))
                .withTag("", organizationID)
                .encodedJson();

        final Bundle rosterSearch = bundleSearchRequest
                .execute();

        assertFalse(rosterSearch.isEmpty(), "Should have roster");
        final Group roster = (Group) rosterSearch.getEntryFirstRep().getResource();

        client
                .delete()
                .resourceById(new IdType(roster.getId()))
                .encodedJson()
                .execute();

        // Make sure it's done
        final IReadExecutable<IBaseResource> rosterReadRequest = client
                .read()
                .resource("Group")
                .withId(roster.getId())
                .encodedJson();
        assertThrows(ResourceNotFoundException.class, rosterReadRequest::execute, "Should not have roster");

        final Bundle emptyBundle = bundleSearchRequest.execute();
        assertEquals(0, emptyBundle.getTotal(), "Should not have found any rosters");

    }

    private static Group createBaseAttributionGroup(String providerNPI, String organizationID) {

        final CodeableConcept attributionConcept = new CodeableConcept();
        attributionConcept.setText("attributed-to");

        final CodeableConcept NPIConcept = new CodeableConcept();
        NPIConcept.setText(providerNPI);
        final Group rosterGroup = new Group();
        rosterGroup.setType(Group.GroupType.PERSON);
        rosterGroup.setActive(true);
        rosterGroup.addCharacteristic()
                .setExclude(false)
                .setCode(attributionConcept)
                .setValue(NPIConcept);
        FHIRBuilders.addOrganizationTag(rosterGroup, UUID.fromString(organizationID));

        return rosterGroup;
    }
}
