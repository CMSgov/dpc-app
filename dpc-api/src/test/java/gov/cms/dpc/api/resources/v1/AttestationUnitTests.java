package gov.cms.dpc.api.resources.v1;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.DPCAPIConfiguration;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.bluebutton.client.BlueButtonClient;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.queue.IJobQueue;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.V3RoleClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
public class AttestationUnitTests {

    private static final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    private static GroupResource groupResource;

    @BeforeAll
    static void setup() {
        final Logger logger = (Logger) LoggerFactory.getLogger(GroupResource.class);

        listAppender.start();
        logger.addAppender(listAppender);
        // Do all the things
        final IJobQueue mockQueue = Mockito.mock(IJobQueue.class);
        final IGenericClient mockClient = Mockito.mock(IGenericClient.class);
        final BlueButtonClient mockBfdClient = Mockito.mock(BlueButtonClient.class);
        groupResource = new GroupResource(mockQueue, mockClient, "http://local.test", mockBfdClient, new DPCAPIConfiguration());
    }

    @BeforeEach
    void setupEach() {
        listAppender.start();
    }

    @AfterEach
    void shutdownEach() {
        listAppender.stop();
        listAppender.list.clear();
    }

    @Test
    void testCreateLogging() {


        final Organization org = new Organization();
        org.setId(new IdType("Organization", APITestHelpers.ORGANIZATION_ID));

        final Group group = new Group();
        group.addMember().setEntity(new Reference("Patient/test-patient"));
        final Provenance provenance = createTestProvenance();


        assertThrows(NullPointerException.class, () -> groupResource.createRoster(new OrganizationPrincipal(org), provenance, group));

        // Check the log worked correctly
        assertEquals(1, listAppender.list.size(), "Should have a logged message");

        // Get the message
        final Coding reason = provenance.getReasonFirstRep();

        final Provenance.ProvenanceAgentComponent performer = FHIRExtractors.getProvenancePerformer(provenance);
        final List<String> attributedPatients = group
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(Reference::getReference)
                .collect(Collectors.toList());
        final String expectedMessage = String.format("Organization %s is attesting a %s purpose between provider %s and patient(s) %s", performer.getWhoReference().getReference(), reason.getCode(), performer.getOnBehalfOfReference().getReference(), attributedPatients);
        assertEquals(expectedMessage, listAppender.list.get(0).getFormattedMessage(), "Should have correct message");
    }

    @Test
    void testUpdateLogging() {
        final Organization org = new Organization();
        org.setId(new IdType("Organization", APITestHelpers.ORGANIZATION_ID));

        final Group group = new Group();
        group.addMember().setEntity(new Reference("Patient/test-patient"));

        final Provenance provenance = createTestProvenance();
        final UUID rosterID = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> groupResource.updateRoster(new OrganizationPrincipal(org), rosterID, provenance, group));

        // Check the log worked correctly
        assertEquals(1, listAppender.list.size(), "Should have a logged message");

        // Get the message
        final Coding reason = provenance.getReasonFirstRep();

        final Provenance.ProvenanceAgentComponent performer = FHIRExtractors.getProvenancePerformer(provenance);
        final List<String> attributedPatients = group
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(Reference::getReference)
                .collect(Collectors.toList());
        final String expectedMessage = String.format("Organization %s is attesting a %s purpose between provider %s and patient(s) %s for roster %s", performer.getWhoReference().getReference(), reason.getCode(), performer.getOnBehalfOfReference().getReference(), attributedPatients, new IdType("Group", rosterID.toString()));
        assertEquals(expectedMessage, listAppender.list.get(0).getFormattedMessage(), "Should have correct message");
    }

    @Test
    void testUpdateLoggingMultiplePatients() {
        final Organization org = new Organization();
        org.setId(new IdType("Organization", APITestHelpers.ORGANIZATION_ID));

        final Group group = new Group();
        group.addMember().setEntity(new Reference("Patient/test-patient"));
        group.addMember().setEntity(new Reference("Patient/test-patient2"));
        group.addMember().setEntity(new Reference("Patient/test-patient3"));

        final Provenance provenance = createTestProvenance();
        final UUID rosterID = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> groupResource.addRosterMembers(new OrganizationPrincipal(org), rosterID, provenance, group));

        // Check the log worked correctly
        assertEquals(1, listAppender.list.size(), "Should have a logged message");

        // Get the message
        final Coding reason = provenance.getReasonFirstRep();

        final Provenance.ProvenanceAgentComponent performer = FHIRExtractors.getProvenancePerformer(provenance);
        final List<String> attributedPatients = group
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(Reference::getReference)
                .collect(Collectors.toList());
        final String expectedMessage = String.format("Organization %s is attesting a %s purpose between provider %s and patient(s) %s for roster %s", performer.getWhoReference().getReference(), reason.getCode(), performer.getOnBehalfOfReference().getReference(), attributedPatients, new IdType("Group", rosterID.toString()));
        assertEquals(expectedMessage, listAppender.list.get(0).getFormattedMessage(), "Should have correct message");
    }

    @Test
    void testRosterAddLogging() {
        final Organization org = new Organization();
        org.setId(new IdType("Organization", APITestHelpers.ORGANIZATION_ID));

        final Group group = new Group();
        group.addMember().setEntity(new Reference("Patient/test-patient"));
        group.addMember().setEntity(new Reference("Patient/test-patient2"));
        group.addMember().setEntity(new Reference("Patient/test-patient3"));

        final Provenance provenance = createTestProvenance();
        final UUID rosterID = UUID.randomUUID();

        assertThrows(NullPointerException.class, () -> groupResource.updateRoster(new OrganizationPrincipal(org), rosterID, provenance, group));

        // Check the log worked correctly
        assertEquals(1, listAppender.list.size(), "Should have a logged message");

        // Get the message
        final Coding reason = provenance.getReasonFirstRep();

        final Provenance.ProvenanceAgentComponent performer = FHIRExtractors.getProvenancePerformer(provenance);
        final List<String> attributedPatients = group
                .getMember()
                .stream()
                .map(Group.GroupMemberComponent::getEntity)
                .map(Reference::getReference)
                .collect(Collectors.toList());
        final String expectedMessage = String.format("Organization %s is attesting a %s purpose between provider %s and patient(s) %s for roster %s", performer.getWhoReference().getReference(), reason.getCode(), performer.getOnBehalfOfReference().getReference(), attributedPatients, new IdType("Group", rosterID.toString()));
        assertEquals(expectedMessage, listAppender.list.get(0).getFormattedMessage(), "Should have correct message");
    }

    private Provenance createTestProvenance() {
        final Provenance provenance = new Provenance();
        provenance.addReason().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT");
        final Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference("Organization/test"));
        agent.setOnBehalfOf(new Reference("Practitioner/test"));
        final Coding roleCode = new Coding();
        roleCode.setSystem(V3RoleClass.AGNT.getSystem());
        roleCode.setCode(V3RoleClass.AGNT.toCode());

        final CodeableConcept roleConcept = new CodeableConcept();
        roleConcept.addCoding(roleCode);
        agent.setRole(Collections.singletonList(roleConcept));
        provenance.addAgent(agent);

        return provenance;
    }
}
