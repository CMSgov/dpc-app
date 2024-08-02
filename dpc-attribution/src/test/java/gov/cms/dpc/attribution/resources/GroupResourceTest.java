package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.attribution.resources.v1.GroupResource;
import gov.cms.dpc.common.utils.SeedProcessor;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.testing.IntegrationTest;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
