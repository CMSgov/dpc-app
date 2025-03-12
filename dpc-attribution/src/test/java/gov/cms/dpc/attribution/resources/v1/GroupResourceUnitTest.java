package gov.cms.dpc.attribution.resources.v1;

import com.google.common.collect.Maps;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.jdbi.*;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.entities.RosterEntity;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.testing.factories.FHIRGroupBuilder;
import gov.cms.dpc.testing.factories.FHIRPatientBuilder;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private DPCAttributionConfiguration configuration;

    private final FHIREntityConverter converter = FHIREntityConverter.initialize();


    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        configuration = new DPCAttributionConfiguration();
        groupResource = new GroupResource(converter, providerDAO, rosterDAO, patientDAO, relationshipDAO, configuration);
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

        configuration.setPatientLimit(10);
        configuration.setExpirationThreshold(10);
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

        configuration.setPatientLimit(10);
        configuration.setExpirationThreshold(10);
        Mockito.when(rosterDAO.findEntities(isNull(),eq(orgId), eq(providerNpi), isNull())).thenReturn(List.of());
        Mockito.when(providerDAO.getProviders(isNull(),eq(providerNpi), eq(orgId))).thenReturn(List.of(new ProviderEntity()));
        patientBank.keySet().forEach(patientId ->
                Mockito.when(patientDAO.patientSearch(eq(patientId), isNull(),eq(orgId))).thenReturn(List.of(new PatientEntity())));

        Mockito.when(patientDAO.patientSearch(eq(badPatientUUID), isNull(),eq(orgId))).thenReturn(List.of());
        Mockito.when(rosterDAO.persistEntity(any(RosterEntity.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        //Act & Assert
        assertThrows(WebApplicationException.class, () -> groupResource.createRoster(group), "Expected and exception if an invalid patient was added");
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
