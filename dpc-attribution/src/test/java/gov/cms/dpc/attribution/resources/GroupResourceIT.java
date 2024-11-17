package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.attribution.resources.v1.GroupResource;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.IntegrationTest;
import gov.cms.dpc.testing.MBIUtil;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static gov.cms.dpc.attribution.AttributionTestHelpers.DEFAULT_ORG_ID;
import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
public class GroupResourceTest extends AbstractAttributionTest {

    private IGenericClient client;

    @BeforeEach
    public void beforeEach() {
        APPLICATION.getConfiguration().setPatientLimit(1);
        client = AttributionTestHelpers.createFHIRClient(ctx, getServerURL());
    }

    @Test
    public void testCreateRosterPatientLimit() {
        final Practitioner practitioner = createPractitioner("1111111112");
        final Patient patient1 = createPatient("0O00O00OO01", DEFAULT_ORG_ID);
        final Patient patient2 = createPatient("0O00O00OO00", DEFAULT_ORG_ID);
        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        //Add too many patients
        group.addMember().setEntity(new Reference(patient1.getIdElement()));
        group.addMember().setEntity(new Reference(patient2.getIdElement()));

        assertThrows(InvalidRequestException.class, () -> client.create()
                .resource(group)
                .encodedJson()
                .execute());

        //Remove second patient
        group.getMember().remove(1);

        //Add the amount of patients allowed
        final MethodOutcome methodOutcome = client.create()
                .resource(group)
                .encodedJson()
                .execute();

        assertTrue(methodOutcome.getCreated());

    }

    @Test
    public void testCreateDuplicateRoster() {
        final Practitioner practitioner = createPractitioner(NPIUtil.generateNPI());
        final Patient patient = createPatient(MBIUtil.generateMBI(), DEFAULT_ORG_ID);

        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);
        group.addMember().setEntity(new Reference(patient.getIdElement()));

        client.create()
            .resource(group)
            .encodedJson()
            .execute();

        // "Create" a new roster for the same provider and org
        assertThrows(ForbiddenOperationException.class, () ->
            client.create()
            .resource(group)
            .encodedJson()
            .execute(), "Should error on a duplicate roster");
    }

    @Test
    public void testReplaceRosterPatientLimit() {
        final Practitioner practitioner = createPractitioner("1211111111");
        final Patient patient1 = createPatient("0O00O00OO02", DEFAULT_ORG_ID);
        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        //Create initial group
        group.addMember().setEntity(new Reference(patient1.getIdElement()));

        final MethodOutcome methodOutcome = client.create()
                .resource(group)
                .encodedJson()
                .execute();
        IIdType groupId = methodOutcome.getResource().getIdElement();

        assertTrue(methodOutcome.getCreated());

        //Add an additional patient
        final Patient patient2 = createPatient("0O00O00OO00", DEFAULT_ORG_ID);
        group.addMember().setEntity(new Reference(patient2.getIdElement()));

        assertThrows(InvalidRequestException.class, () -> client.update()
                .resource(group)
                .withId(groupId)
                .encodedJson()
                .execute());

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
        final Group groupForParams = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);
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
        final Group groupForCreate = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        final MethodOutcome methodOutcome = client.create()
                .resource(groupForCreate)
                .encodedJson()
                .execute();
        assertTrue(methodOutcome.getCreated());

        final Group createdGroup = (Group) methodOutcome.getResource();
        final Group groupForUpdate = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);
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
    public void testAddMembersToRosterPatientLimit() {
        final Practitioner practitioner = createPractitioner("1112111111");
        final Patient patient1 = createPatient("0O00O00OO03", DEFAULT_ORG_ID);
        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        //Create initial group
        group.addMember().setEntity(new Reference(patient1.getIdElement()));

        final MethodOutcome methodOutcome = client.create()
                .resource(group)
                .encodedJson()
                .execute();

        assertTrue(methodOutcome.getCreated());
        final Group createdGroup = (Group) methodOutcome.getResource();

        //Add new patient to existing group, should throw error
        final Patient patient2 = createPatient("0O00O00OO10", DEFAULT_ORG_ID);
        group.addMember().setEntity(new Reference(patient2.getIdElement()));

        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(group);

        assertThrows(InvalidRequestException.class, () -> client
                .operation()
                .onInstance(createdGroup.getIdElement())
                .named("$add")
                .withParameters(parameters)
                .returnResourceType(Group.class)
                .encodedJson()
                .execute());

        //Add same patient to existing group, should not throw an error nor should it update any members

        group.getMember().clear();
        group.addMember().setEntity(new Reference(patient1.getIdElement()));

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
    public void testRosterSizeToBigMethodDirectly() {
        final Practitioner practitioner1 = AttributionTestHelpers.createPractitionerResource("1111111112");
        final Group group1 = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner1), DEFAULT_ORG_ID);

        assertFalse(GroupResource.rosterSizeTooBig(-1, group1));
        assertFalse(GroupResource.rosterSizeTooBig(null, group1));
        assertFalse(GroupResource.rosterSizeTooBig(1, (Group) null));
        assertFalse(GroupResource.rosterSizeTooBig(1, group1, null));

        group1.getMember().clear();
        group1.addMember().setEntity(new Reference("1"));
        assertFalse(GroupResource.rosterSizeTooBig(1, group1));

        group1.getMember().clear();
        group1.addMember().setEntity(new Reference("1"));
        group1.addMember().setEntity(new Reference("1"));
        assertFalse(GroupResource.rosterSizeTooBig(1, group1));

        group1.getMember().clear();
        group1.addMember().setEntity(new Reference("1"));
        group1.addMember().setEntity(new Reference("2"));
        assertTrue(GroupResource.rosterSizeTooBig(1, group1));

        final Practitioner practitioner2 = AttributionTestHelpers.createPractitionerResource("1211111111");
        final Group group2 = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner2), DEFAULT_ORG_ID);
        group1.getMember().clear();
        group1.addMember().setEntity(new Reference("1"));
        group1.addMember().setEntity(new Reference("2"));
        group2.getMember().clear();
        group2.addMember().setEntity(new Reference("1"));
        group2.addMember().setEntity(new Reference("2"));
        assertTrue(GroupResource.rosterSizeTooBig(1, group1, group2));

        group1.getMember().clear();
        group1.addMember().setEntity(new Reference("1"));
        group2.getMember().clear();
        group2.addMember().setEntity(new Reference("1"));
        assertFalse(GroupResource.rosterSizeTooBig(1, group1, group2));

        group1.getMember().clear();
        group1.addMember().setEntity(new Reference("1"));
        group2.getMember().clear();
        group2.addMember().setEntity(new Reference("2"));
        assertTrue(GroupResource.rosterSizeTooBig(1, group1, group2));
    }

    @Test
    public void testMaxPatients() {
        final int MAX_PATIENTS = 1350;
        APPLICATION.getConfiguration().setPatientLimit(MAX_PATIENTS);

        final Practitioner practitioner = createPractitioner(NPIUtil.generateNPI());
        final Group group = SeedProcessor.createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        // Create patients
        List<Patient> patientList = new LinkedList<>();
        for(int i=0; i<MAX_PATIENTS; i++) {
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
        assertEquals(MAX_PATIENTS, groupResult.getMember().size());
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
