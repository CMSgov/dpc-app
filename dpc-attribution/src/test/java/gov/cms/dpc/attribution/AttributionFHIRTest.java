package gov.cms.dpc.attribution;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.gclient.*;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRBuilders;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.IntegrationTest;
import gov.cms.dpc.testing.OrganizationHelpers;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import org.apache.commons.lang3.tuple.Pair;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gov.cms.dpc.attribution.SharedMethods.submitAttributionBundle;
import static gov.cms.dpc.common.utils.SeedProcessor.createBaseAttributionGroup;
import static org.junit.jupiter.api.Assertions.*;

@Disabled
@ExtendWith(BufferedLoggerHandler.class)
@IntegrationTest
class AttributionFHIRTest {

    private static final String KEY_PREFIX = "dpc.attribution";
    private static final DropwizardTestSupport<DPCAttributionConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAttributionService.class, "ci.application.conf", ConfigOverride.config("server.applicationConnectors[0].port", "3727"),
            ConfigOverride.config(KEY_PREFIX, "logging.level", "ERROR"));
    private static final FhirContext ctx = FhirContext.forDstu3();
    private static final String CSV = "test_associations-dpr.csv";
    private static Map<String, List<Pair<String, String>>> groupedPairs = new HashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static Organization organization;

    @BeforeAll
    static void setup() throws Exception {
        APPLICATION.before();
        APPLICATION.getApplication().run("db", "drop-all", "--confirm-delete-everything", "ci.application.conf");
        APPLICATION.getApplication().run("db", "migrate", "ci.application.conf");

        // Get the test seeds
        final InputStream resource = AttributionFHIRTest.class.getClassLoader().getResourceAsStream(CSV);
        if (resource == null) {
            throw new MissingResourceException("Can not find seeds file", AttributionFHIRTest.class.getName(), CSV);
        }

        // Read in the seeds and create the 'Roster' bundle
        groupedPairs = SeedProcessor.extractProviderMap(resource);

        // Create the Organization
        organization = OrganizationHelpers.createOrganization(ctx, AttributionTestHelpers.createFHIRClient(ctx, String.format("http://localhost:%s/v1/", APPLICATION.getLocalPort())));
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
                .map((Map.Entry<String, List<Pair<String, String>>> entry) -> SeedProcessor.generateAttributionBundle(entry, orgID))
                .flatMap((bundle) -> Stream.of(
                        DynamicTest.dynamicTest(nameGenerator.apply(bundle, "Submit"), () -> submitRoster(bundle)),
                        DynamicTest.dynamicTest(nameGenerator.apply(bundle, "Update"), () -> updateRoster(bundle)),
                        DynamicTest.dynamicTest(nameGenerator.apply(bundle, "Remove"), () -> removeRoster(bundle))));
    }

    private void submitRoster(Bundle bundle) {
        final Practitioner practitioner = (Practitioner) bundle.getEntryFirstRep().getResource();
        final String providerID = practitioner.getIdentifierFirstRep().getValue();
        final String organizationID = FHIRExtractors.getOrganizationID(practitioner);

        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        client.registerInterceptor(loggingInterceptor);

        final Group createdGroup = submitAttributionBundle(client, bundle);

        // Get the patients
        final IReadExecutable<Group> groupSizeQuery = client
                .read()
                .resource(Group.class)
                .withId(createdGroup.getId())
                .encodedJson();

        final Group fetchedGroup = groupSizeQuery
                .execute();
        // Remove meta so we can do equality between the two resources
        fetchedGroup.setMeta(null);

        assertAll(() -> assertTrue(createdGroup.equalsDeep(fetchedGroup), "Groups should be equal"),
                () -> assertEquals(bundle.getEntry().size() - 1, fetchedGroup.getMember().size(), "Should have the same number of beneies"));

        final String patientID = bundle.getEntry().get(1).getResource().getId();

        final Bundle searchedPatient = client
                .search()
                .forResource(Group.class)
                .where(Group.MEMBER.hasId(patientID))
                .where(buildCharacteristicSearch(providerID))
                .withTag("", organizationID)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, searchedPatient.getTotal(), "Should only have a single group");

        // Resubmit group and make sure the size doesn't change
        // Re-add the meta, because it gets stripped
        FHIRBuilders.addOrganizationTag(createdGroup, UUID.fromString(organizationID));
        client
                .create()
                .resource(createdGroup)
                .encodedJson().execute();

        final Group group2 = groupSizeQuery.execute();
        assertAll(() -> assertTrue(fetchedGroup.equalsDeep(group2), "Groups should be equal"),
                () -> assertEquals(bundle.getEntry().size() - 1, group2.getMember().size(), "Should have the same number of beneies"));

        // Try to get attributed patients
        final Bundle attributed = client
                .operation()
                .onInstance(group2.getIdElement())
                .named("patients")
                .withNoParameters(Parameters.class)
                .useHttpGet()
                .encodedJson()
                .returnResourceType(Bundle.class)
                .execute();

        assertEquals(group2.getMember().size(), attributed.getTotal(), "Should have the same number of patients");

        // Try to get a non-existent roster

        final IReadExecutable<Group> badRead = client
                .read()
                .resource(Group.class)
                .withId(UUID.randomUUID().toString())
                .encodedJson();

        assertThrows(ResourceNotFoundException.class, badRead::execute, "Should not have found roster");
    }


    private void updateRoster(Bundle bundle) {

        final Practitioner practitioner = (Practitioner) bundle.getEntryFirstRep().getResource();
        final String providerID = practitioner.getIdentifierFirstRep().getValue();
        final String organizationID = FHIRExtractors.getOrganizationID(practitioner);

        // Create the new patient and submit them
        final Patient patient = new Patient();
        final Identifier patientIdentifier = new Identifier();
        patientIdentifier.setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("0I00I00II00");
        patient.addIdentifier(patientIdentifier);
        patient.addName().addGiven("New Test Patient");
        patient.setBirthDate(new GregorianCalendar(2019, Calendar.MARCH, 1).getTime());
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);
        patient.setManagingOrganization(new Reference(new IdType("Organization", organizationID)));

        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        client.registerInterceptor(loggingInterceptor);

        final MethodOutcome patientCreated = client
                .create()
                .resource(patient)
                .encodedJson()
                .execute();

        final Patient newPatient = (Patient) patientCreated.getResource();

        // Find the existing Roster ID
        final Bundle searchedProviderGroup = client
                .search()
                .forResource(Group.class)
                .where(buildCharacteristicSearch(providerID))
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

        final Parameters addParam = new Parameters();
        addParam.addParameter().setResource(newRoster);

        // Update the roster
        final IOperationUntypedWithInput<Parameters> addMemberRequest = client
                .operation()
                .onInstance(new IdType(groupID))
                .named("add")
                .withParameters(addParam)
                .encodedJson();

        addMemberRequest.execute();

        // Check how many are attributed
        final IReadExecutable<Group> getUpdatedGroup = client
                .read()
                .resource(Group.class)
                .withId(groupID)
                .encodedJson();
        final Group updatedGroup = getUpdatedGroup
                .execute();

        assertEquals(bundle.getEntry().size(), updatedGroup.getMember().size(), "Should have an additional patient");

        final String patientID = newPatient.getId();
        final Bundle searchedPatient = client
                .search()
                .forResource(Group.class)
                .where(Group.MEMBER.hasId(patientID))
                .where(buildCharacteristicSearch(providerID))
                .withTag("", organizationID)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, searchedPatient.getTotal(), "Should only have a single group");

        // Remove the patient
        final Group.GroupMemberComponent removeEntity = new Group.GroupMemberComponent().setEntity(patientReference).setInactive(true);
        newRoster.setMember(List.of(removeEntity));
        final Parameters removeParams = new Parameters();
        removeParams.addParameter().setResource(newRoster);

        final IOperationUntypedWithInput<Parameters> removeMemberRequest = client
                .operation()
                .onInstance(new IdType(groupID))
                .named("remove")
                .withParameters(removeParams)
                .encodedJson();

        removeMemberRequest.execute();

        // Count inactive users
        final List<Group.GroupMemberComponent> members = getUpdatedGroup.execute().getMember();

        final List<Group.GroupMemberComponent> inactiveMembers = members
                .stream()
                .filter(Group.GroupMemberComponent::getInactive)
                .collect(Collectors.toList());

        final List<Group.GroupMemberComponent> activeMembers = members
                .stream()
                .filter(member -> !member.getInactive())
                .collect(Collectors.toList());

        // Add 10 minutes to avoid comparison differences with milliseconds on the Date values
        // Since we're only comparing Date values, adding a minute offset ensure the test passes, but is still valid
        final Instant nowInst = Instant.now();
        final Date offsetNow = Date.from(nowInst.plus(10, ChronoUnit.MINUTES));

        assertAll(() -> assertEquals(bundle.getEntry().size(), members.size(), "Should  have the same total members"),
                () -> assertEquals(bundle.getEntry().size() - 1, activeMembers.size(), "Should have 1 less active member"),
                () -> assertEquals(1, inactiveMembers.size(), "Should have a single inactive"),
                () -> assertEquals(1, offsetNow.compareTo(inactiveMembers.get(0).getPeriod().getEnd()), "Period end should be today"),
                () -> assertEquals(-1, offsetNow.compareTo(activeMembers.get(0).getPeriod().getEnd()), "Active member should have period end after today"),
                () -> assertNotEquals(activeMembers.get(0).getPeriod().getStart(), activeMembers.get(0).getPeriod().getEnd(), "Period should not be equal"));

        // Re-add the patient, which should force them to be re-created
        final Group.GroupMemberComponent reAddEntity = new Group.GroupMemberComponent().setEntity(patientReference);
        newRoster.setMember(List.of(reAddEntity));
        final Parameters reAddParams = new Parameters();
        reAddParams.addParameter().setResource(newRoster);

        final IOperationUntypedWithInput<Group> reAddMemberRequest = client
                .operation()
                .onInstance(new IdType(groupID))
                .named("add")
                .withParameters(reAddParams)
                .returnResourceType(Group.class)
                .encodedJson();

        // This should not throw an exception, when re-adding the member
        final Group group = reAddMemberRequest.execute();

        final Group.GroupMemberComponent matchingMember = group
                .getMember()
                .stream()
                .filter(member -> member.getEntity().getReference().equals(patientID))
                .findAny()
                .orElseThrow(() -> new IllegalStateException("Should have matching patient"));

        // We should compare the created dates, but Java Date objects don't carry enough precision for us to do an accurate comparison
        assertFalse(matchingMember.getInactive(), "Member should be active");

        // Replace the roster and ensure the numbers are correct.
        client
                .update()
                .resource(newRoster)
                .withId(new IdType(groupID))
                .encodedJson()
                .execute();

        assertEquals(1,
                getUpdatedGroup.execute().getMember().size(),
                "Should only have a single member");
    }

    private void removeRoster(Bundle bundle) {

        final Practitioner practitioner = (Practitioner) bundle.getEntryFirstRep().getResource();
        final String providerID = practitioner.getIdentifierFirstRep().getValue();
        final String organizationID = FHIRExtractors.getOrganizationID(practitioner);
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient("http://localhost:" + APPLICATION.getLocalPort() + "/v1/");

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        client.registerInterceptor(loggingInterceptor);

        final IQuery<Bundle> bundleSearchRequest = client
                .search()
                .forResource(Group.class)
                .returnBundle(Bundle.class)
                .where(buildCharacteristicSearch(providerID))
                .withTag("", organizationID)
                .encodedJson();

        final Bundle rosterSearch = bundleSearchRequest
                .execute();

        assertTrue(rosterSearch.getTotal() > 0, "Should have roster");
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

    private static ICriterion<TokenClientParam> buildCharacteristicSearch(String providerID) {
        return Group.CHARACTERISTIC_VALUE
                .withLeft(Group.CHARACTERISTIC.exactly().code("attributed-to"))
                .withRight(Group.VALUE.exactly().systemAndCode(DPCIdentifierSystem.NPPES.getSystem(), providerID));
    }
}
