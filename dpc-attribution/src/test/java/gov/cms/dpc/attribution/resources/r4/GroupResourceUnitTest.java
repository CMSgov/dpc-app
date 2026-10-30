package gov.cms.dpc.attribution.resources.r4;

import com.google.common.collect.Maps;
import gov.cms.dpc.attribution.jdbi.PageResult;
import gov.cms.dpc.attribution.jdbi.r4.PatientDAO;
import gov.cms.dpc.attribution.jdbi.r4.ProviderDAO;
import gov.cms.dpc.attribution.jdbi.r4.RelationshipDAO;
import gov.cms.dpc.attribution.jdbi.r4.RosterDAO;
import gov.cms.dpc.common.entities.r4.AttributionRelationship;
import gov.cms.dpc.common.entities.r4.PatientEntity;
import gov.cms.dpc.common.entities.r4.ProviderEntity;
import gov.cms.dpc.common.entities.r4.RosterEntity;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.testing.factories.r4.FHIRGroupBuilder;
import gov.cms.dpc.testing.factories.r4.FHIRPatientBuilder;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static gov.cms.dpc.attribution.utils.r4.AttributionTestHelpers.queryMatches;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

class GroupResourceUnitTest {

    private GroupResource groupResource;

    @Mock
    ProviderDAO providerDAO;

    @Mock
    PatientDAO patientDAO;

    @Mock
    RosterDAO rosterDAO;

    @Mock
    RelationshipDAO relationshipDAO;

    private final FHIREntityConverter converter = FHIREntityConverter.initialize();


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        groupResource = new GroupResource(converter, providerDAO, rosterDAO, patientDAO, relationshipDAO);
    }


    @Test
    void testCreateRosterHappyCase(){
        //Arrange
        final UUID orgId = UUID.randomUUID();
        final String providerNpi = NPIUtil.generateNPI();

        final Map<UUID,Patient> patientBank = makeTestPatients(5);

        final Group group = FHIRGroupBuilder
                .newBuild()
                .withUUID()
                .attributedTo(providerNpi)
                .withPatients(patientBank.keySet().toArray(UUID[]::new))
                .withOrgTag(orgId)
                .build();

        Mockito.when(rosterDAO.findEntities(isNull(),eq(orgId), eq(providerNpi), isNull())).thenReturn(List.of());
        Mockito.when(providerDAO.getProviders(isNull(),eq(providerNpi), eq(orgId))).thenReturn(List.of(new ProviderEntity()));
        Mockito.when(patientDAO.bulkPatientSearchById(eq(orgId), any(List.class))).thenReturn(Collections.nCopies(5, new PatientEntity()));

        Mockito.when(rosterDAO.persistEntity(any(RosterEntity.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        //Act
        Response response = groupResource.createRoster(group);

        //Assert
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus(), "Response status should have been CREATED (201)");
        assertNotNull(response.getEntity(), "Response should have contained a body");
        assertEquals(patientBank.keySet().size(),((Group) response.getEntity()).getMember().size(), "Patients count should be the same as submitted");
        assertNull(((Group) response.getEntity()).getMember().getFirst().getPeriod().getEnd());
    }

    @Test
    void testCreateRosterWithInvalidPatient(){
        //Arrange
        final UUID orgId = UUID.randomUUID();
        final String providerNpi = NPIUtil.generateNPI();

        final Map<UUID,Patient> patientBank = makeTestPatients(5);

        final UUID badPatientUUID = UUID.randomUUID();

        final Group group = FHIRGroupBuilder
                .newBuild()
                .attributedTo(providerNpi)
                .withPatients(patientBank.keySet().toArray(UUID[]::new))
                .withPatients(badPatientUUID)
                .withOrgTag(orgId)
                .build();

        Mockito.when(rosterDAO.findEntities(isNull(),eq(orgId), eq(providerNpi), isNull())).thenReturn(List.of());
        Mockito.when(providerDAO.getProviders(isNull(),eq(providerNpi), eq(orgId))).thenReturn(List.of(new ProviderEntity()));

        for (UUID patientId : patientBank.keySet()) {
            Mockito.when(patientDAO.patientSearch(argThat(queryMatches(patientId, orgId, null, null))))
                    .thenReturn(new PageResult<> (
                            List.of(new PatientEntity()),
                            false
                    ));
        }

        Mockito.when(patientDAO.patientSearch(argThat(queryMatches(badPatientUUID, orgId, null, null)))).thenReturn(new PageResult<> (List.of(), false));
        Mockito.when(rosterDAO.persistEntity(any(RosterEntity.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        //Act & Assert
        assertThrows(WebApplicationException.class, () -> groupResource.createRoster(group), "Expected and exception if an invalid patient was added");
    }

    @Test
    void testReactivatingClearsExpirationDt() {
        final UUID rosterId = UUID.randomUUID();
        final UUID organizationId = UUID.randomUUID();
        final UUID patientId = UUID.randomUUID();
        final String providerNpi = NPIUtil.generateNPI();
        final PatientEntity patient = new PatientEntity();
        patient.setID(patientId);
        final ProviderEntity provider = new ProviderEntity();
        provider.setProviderNPI(providerNpi);
        final RosterEntity roster = new RosterEntity();
        roster.setId(rosterId);
        roster.setAttributedProvider(provider);

        final AttributionRelationship relationship = new AttributionRelationship(roster, patient,
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));
        relationship.setInactive(true);
        relationship.setPeriodEnd(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        roster.setAttributions(new ArrayList<>(List.of(relationship)));

        final Group groupUpdate = FHIRGroupBuilder.newBuild()
                .withUUID()
                .attributedTo(providerNpi)
                .withPatients(patientId)
                .withOrgTag(organizationId)
                .build();

        Mockito.when(rosterDAO.getEntity(rosterId)).thenReturn(Optional.of(roster));
        Mockito.when(patientDAO.bulkPatientSearchById(eq(organizationId), anyList())).thenReturn(List.of(patient));
        Mockito.when(relationshipDAO.lookupAttributionRelationships(rosterId, List.of(patientId))).thenReturn(List.of(relationship));

        final Group updatedGroup = groupResource.addRosterMembers(rosterId, groupUpdate);

        assertFalse(updatedGroup.getMember().getFirst().getInactive());
        assertNull(updatedGroup.getMember().getFirst().getPeriod().getEnd());
    }

    private Map<UUID,Patient> makeTestPatients(int count){
        if(count>88){
            throw new IllegalStateException("Don't support building more than 88 patients..yet (need a better mbi generator)");
        }
        final Map<UUID,Patient> patients = Maps.newHashMap();
        while(count>0){
            UUID id = UUID.randomUUID();
            Patient patient = FHIRPatientBuilder
                    .newBuild()
                    .withMbi("4S41C00AA"+(count+10)) //makes MBI in range 4S41C00AA10 -> 4S41C00AA99
                    .withId(id)
                    .withTestData() //Will eventually be random test data.
                    .build();

            patients.put(id,patient);
            count--;
        }
        return patients;
    }
}
