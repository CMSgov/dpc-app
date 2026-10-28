package gov.cms.dpc.attribution.resources.r4;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.attribution.utils.r4.AbstractAttributionTest;
import gov.cms.dpc.attribution.utils.r4.AttributionTestHelpers;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.r4.FHIRExtractors;
import gov.cms.dpc.testing.IntegrationTest;
import gov.cms.dpc.testing.utils.MBIUtil;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static gov.cms.dpc.attribution.utils.r4.AttributionTestHelpers.DEFAULT_ORG_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@IntegrationTest
public class GroupResourceTest extends AbstractAttributionTest {

    private IGenericClient client;

    @BeforeEach
    public void beforeEach() {
        client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());
    }

    @Test
    public void testReplaceRosterMembers() {
        final Practitioner practitioner = createPractitioner("1211111111");
        final Patient patient1 = createPatient("0O00O00OO02", DEFAULT_ORG_ID);
        final Group group = AttributionTestHelpers.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        //Create initial group
        group.addMember().setEntity(new Reference(patient1.getIdElement()));

        final MethodOutcome methodOutcome = client.create()
                .resource(group)
                .encodedJson()
                .execute();
        IIdType groupId = methodOutcome.getResource().getIdElement();

        assertTrue(methodOutcome.getCreated());

        final Patient patient2 = createPatient("0O00O00OO00", DEFAULT_ORG_ID);

        //Replace patient
        group.getMember().clear();
        group.addMember().setEntity(new Reference(patient2.getIdElement()));

        final MethodOutcome methodOutcomeUpdate = client.update()
                .resource(group)
                .withId(methodOutcome.getResource().getIdElement())
                .encodedJson()
                .execute();

        final Group updatedGroup = client.read()
            .resource(Group.class)
            .withId(groupId)
            .encodedJson()
            .execute();

        assertEquals(patient2.getIdElement().getValueAsString(), updatedGroup.getMemberFirstRep().getEntity().getReference());
    }

    /**
     * When $add is called and a new patient is added to a roster, it should show up in the response
     */
    @Test
    public void testAddToRosterResponse() {
        final Practitioner practitioner = createPractitioner(NPIUtil.generateNPI());
        final Patient patient1 = createPatient("0O00O00OO04", DEFAULT_ORG_ID);
        final Group groupForParams = AttributionTestHelpers.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);
        final MethodOutcome methodOutcome = client.create()
                .resource(groupForParams)
                .encodedJson()
                .execute();
        assertTrue(methodOutcome.getCreated());
        final Group createdGroup = (Group) methodOutcome.getResource();

        final Parameters parametersNoPatient = new Parameters();
        parametersNoPatient.addParameter().setResource(groupForParams);
        Group addMemberResponse = client
                .operation()
                .onInstance(createdGroup.getIdElement())
                .named("$add")
                .withParameters(parametersNoPatient)
                .returnResourceType(Group.class)
                .encodedJson()
                .execute();
        assertEquals(0, addMemberResponse.getMember().size());

        groupForParams.addMember().setEntity(new Reference(patient1.getIdElement()));
        final Parameters parametersWithPatient = new Parameters();
        parametersWithPatient.addParameter().setResource(groupForParams);

        addMemberResponse = client
                .operation()
                .onInstance(createdGroup.getIdElement())
                .named("$add")
                .withParameters(parametersNoPatient)
                .returnResourceType(Group.class)
                .encodedJson()
                .execute();
        assertEquals(1, addMemberResponse.getMember().size());
    }

    /**
     * When replace roster endpoint called, new members PUT into roster should show up in response.
     * This test calls the replaceRoster() endpoint with 0 members for update and 1 member for update
     * in order to ensure the response includes the appropriate number of members
     */
    @Test
    public void testReplaceRosterResponse() {
        final Practitioner practitioner = createPractitioner(NPIUtil.generateNPI());
        final Group groupForCreate = AttributionTestHelpers.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        final MethodOutcome methodOutcome = client.create()
                .resource(groupForCreate)
                .encodedJson()
                .execute();
        assertTrue(methodOutcome.getCreated());

        final Group createdGroup = (Group) methodOutcome.getResource();
        final Group groupForUpdate = AttributionTestHelpers.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);
        final MethodOutcome replaceRosterResponse = client
                .update()
                .resource(groupForUpdate)
                .withId(createdGroup.getIdElement())
                .execute();
        final Group replacementGroup = (Group) replaceRosterResponse.getResource();
        assertEquals(0, replacementGroup.getMember().size());

        final Patient patient1 = createPatient("0O00O00OO05", DEFAULT_ORG_ID);
        groupForUpdate.addMember().setEntity(new Reference(patient1.getIdElement()));
        final MethodOutcome replaceRosterResponse2 = client
                .update()
                .resource(groupForUpdate)
                .withId(replacementGroup.getIdElement())
                .execute();
        final Group replacementGroup2 = (Group) replaceRosterResponse2.getResource();
        assertEquals(1, replacementGroup2.getMember().size());
    }

    @Test
    public void testAddExistingMemberToRoster() {
        final Practitioner practitioner = createPractitioner("1112111111");
        final Patient patient1 = createPatient("0O00O00OO03", DEFAULT_ORG_ID);
        final Group group = AttributionTestHelpers.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        //Create initial group
        group.addMember().setEntity(new Reference(patient1.getIdElement()));

        final MethodOutcome methodOutcome = client.create()
                .resource(group)
                .encodedJson()
                .execute();

        assertTrue(methodOutcome.getCreated());
        final Group createdGroup = (Group) methodOutcome.getResource();

        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(group);

        // patient is already in group, this doesn't increase member size
        Group addMember = client
                .operation()
                .onInstance(createdGroup.getIdElement())
                .named("$add")
                .withParameters(parameters)
                .returnResourceType(Group.class)
                .encodedJson()
                .execute();

        assertEquals(1, addMember.getMember().size());
        assertEquals(patient1.getIdElement().getValueAsString(), addMember.getMemberFirstRep().getEntity().getReference());

    }

    @Test
    public void testLargeRoster() {
        final int LARGE_ROSTER_SIZE = 2213; // Some random number > 2k patients.  (Our current chunk size is 1,000)

        final Practitioner practitioner = createPractitioner(NPIUtil.generateNPI());
        final Group group = AttributionTestHelpers.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        // Create patients
        List<Patient> patientList = new LinkedList<>();
        for(int i=0; i<LARGE_ROSTER_SIZE; i++) {
            Patient patient = AttributionTestHelpers.createPatientResource(MBIUtil.generateMBI(), DEFAULT_ORG_ID);
            patientList.add(patient);
        }
        List<Patient> insertedPatientList = bulkSubmitPatients(patientList);

        // Create roster around patients
        insertedPatientList.forEach(patient -> group.addMember().setEntity(new Reference(patient)));
        MethodOutcome outcomeInsert = client.create()
            .resource(group)
            .encodedJson()
            .execute();

        Group createdGroup = (Group) outcomeInsert.getResource();

        // Update all patients in roster
        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(group);

        Group groupResult = client.operation()
            .onInstance(createdGroup.getIdElement())
            .named("$add")
            .withParameters(parameters)
            .returnResourceType(Group.class)
            .encodedJson()
            .execute();

        // If nothing blew up we should be good, but check a few things anyway
        assertEquals(LARGE_ROSTER_SIZE, groupResult.getMember().size());
        assertEquals(createdGroup.getId(), groupResult.getId());
    }

    private Practitioner createPractitioner(String NPI) {
        final Practitioner practitioner = AttributionTestHelpers.createPractitionerResource(NPI);
        MethodOutcome methodOutcome = client.create()
                .resource(practitioner)
                .encodedJson()
                .execute();
        return (Practitioner) methodOutcome.getResource();
    }

    private Patient createPatient(String MBI, String orgID) {
        final Patient patient = AttributionTestHelpers.createPatientResource(MBI, orgID);
        MethodOutcome methodOutcome = client.create()
                .resource(patient)
                .encodedJson()
                .execute();
        return (Patient) methodOutcome.getResource();
    }

    private List<Patient> bulkSubmitPatients(List<Patient> patients) {
        Bundle patientBundle = new Bundle();
        for (Patient patient : patients) {
            patientBundle.addEntry(new Bundle.BundleEntryComponent().setResource(patient));
        }

        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(patientBundle);

        Bundle patientsReturned = client.operation()
            .onType("Patient")
            .named("submit")
            .withParameters(parameters)
            .returnResourceType(Bundle.class)
            .encodedJson()
            .execute();

        return patientsReturned.getEntry().stream()
            .map(component -> (Patient) component.getResource())
            .collect(Collectors.toList());
    }
}
