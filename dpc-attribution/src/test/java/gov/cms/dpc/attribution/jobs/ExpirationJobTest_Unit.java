package gov.cms.dpc.attribution.jobs;

import gov.cms.dpc.attribution.AbstractAttributionDAOTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.attribution.jdbi.*;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import io.dropwizard.db.ManagedDataSource;
import org.hibernate.Session;
import org.jooq.conf.RenderQuotedNames;
import org.jooq.conf.Settings;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionContext;


import java.lang.reflect.Field;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for verifying that the expiration jobs runs correctly.
 * We currently don't have a way of verifying that the job runs when expected, since we can't really override Dropwizard's time source.
 * In the future, we might consider using something like ByteBuddy to intercept all system time calls and see if the job still gets run.
 * <p>
 * Disabled until made effective
 */
@ExtendWith(BufferedLoggerHandler.class)
@ExtendWith(MockitoExtension.class)
class ExpirationJobTestUnit extends AbstractAttributionDAOTest {

    private OrganizationDAO organizationDAO;
    private ProviderDAO providerDAO;
    private RosterDAO rosterDAO;
    private PatientDAO patientDAO;
    private RelationshipDAO relationshipDAO;

    @Mock
    private ManagedDataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private JobExecutionContext jobContext;
    @Mock
    private Settings settings;
    private static final String PROVIDER_ID = "2322222227";
    @InjectMocks
    private ExpireAttributions expireAttributions;

    @BeforeEach
    public void setUp() throws SQLException, NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);

        Session session = db.getSessionFactory().openSession();
        this.connection = session.doReturningWork(connection1 -> connection1);
        when(this.dataSource.getConnection()).thenReturn(this.connection);

        Field dataSourceField = ExpireAttributions.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        dataSourceField.set(expireAttributions, this.dataSource);

        this.settings = new Settings().withRenderFormatted(true).withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_UNQUOTED);
        Field settingsField = ExpireAttributions.class.getDeclaredField("settings");
        settingsField.setAccessible(true);
        settingsField.set(expireAttributions, this.settings);

        DPCManagedSessionFactory dpcManagedSessionFactory = new DPCManagedSessionFactory(db.getSessionFactory());
        relationshipDAO = new RelationshipDAO(dpcManagedSessionFactory);
        patientDAO = new PatientDAO(dpcManagedSessionFactory);
        organizationDAO = new OrganizationDAO(dpcManagedSessionFactory);
        providerDAO = new ProviderDAO(dpcManagedSessionFactory);
        rosterDAO = new RosterDAO(dpcManagedSessionFactory);
    }

    @Test
    void testExpireAttribution() throws NoSuchFieldException, SQLException {
        OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();
        ProviderEntity provider = AttributionTestHelpers.createProviderEntity(org);
        RosterEntity roster = AttributionTestHelpers.createRosterEntity(org, provider);
        PatientEntity patient = AttributionTestHelpers.createPatientEntity(org);
        AttributionRelationship attr = AttributionTestHelpers.createAttributionRelationship(roster, patient);

        db.inTransaction(() -> {
            organizationDAO.registerOrganization(org);
            patientDAO.persistPatient(patient);
            providerDAO.persistProvider(provider);
            rosterDAO.persistEntity(roster);
            relationshipDAO.addAttributionRelationship(attr);
        });

        this.expireAttributions.doJob(this.jobContext);

        db.inTransaction(() -> {
            Optional<AttributionRelationship> attrib = relationshipDAO.lookupAttributionRelationship(roster.getId(), patient.getID());
            relationshipDAO.refresh(attrib.get());
            assertTrue(attrib.get().isInactive());
        });
    }

    @Test
    void testDeleteInactiveAttribution() {
        OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();
        ProviderEntity provider = AttributionTestHelpers.createProviderEntity(org);
        RosterEntity roster = AttributionTestHelpers.createRosterEntity(org, provider);
        PatientEntity patient = AttributionTestHelpers.createPatientEntity(org);
        AttributionRelationship attr = AttributionTestHelpers.createAttributionRelationship(roster, patient);
        attr.setInactive(true);
        attr.setPeriodEnd(OffsetDateTime.now().minusMonths(6));

        db.inTransaction(() -> {
            organizationDAO.registerOrganization(org);
            patientDAO.persistPatient(patient);
            providerDAO.persistProvider(provider);
            rosterDAO.persistEntity(roster);
            relationshipDAO.addAttributionRelationship(attr);
        });

        this.expireAttributions.doJob(this.jobContext);

        db.inTransaction(() -> {
            Optional<AttributionRelationship> attrib = relationshipDAO.lookupAttributionRelationship(roster.getId(), patient.getID());
            assertTrue(attrib.isEmpty());
        });
    }

    @Test
    void testMultipleAttributions() {
        OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();
        ProviderEntity provider = AttributionTestHelpers.createProviderEntity(org);
        RosterEntity roster = AttributionTestHelpers.createRosterEntity(org, provider);
        PatientEntity patient1 = AttributionTestHelpers.createPatientEntity(org);
        PatientEntity patient2 = AttributionTestHelpers.createPatientEntity(org);
        PatientEntity patient3 = AttributionTestHelpers.createPatientEntity(org);

        // should expire
        AttributionRelationship attr1 = AttributionTestHelpers.createAttributionRelationship(roster, patient1);

        // should get deleted
        AttributionRelationship attr2 = AttributionTestHelpers.createAttributionRelationship(roster, patient2);
        attr2.setInactive(true);
        attr2.setPeriodEnd(OffsetDateTime.now().minusMonths(6));

        // should not be expired or deleted
        AttributionRelationship attr3 = AttributionTestHelpers.createAttributionRelationship(roster, patient3);
        attr3.setPeriodEnd(OffsetDateTime.now().plusMonths(2));

        db.inTransaction(() -> {
            organizationDAO.registerOrganization(org);
            patientDAO.persistPatient(patient1);
            patientDAO.persistPatient(patient2);
            patientDAO.persistPatient(patient3);
            providerDAO.persistProvider(provider);
            rosterDAO.persistEntity(roster);
            relationshipDAO.addAttributionRelationship(attr1);
            relationshipDAO.addAttributionRelationship(attr2);
            relationshipDAO.addAttributionRelationship(attr3);
        });

        this.expireAttributions.doJob(this.jobContext);

        db.inTransaction(() -> {
            Optional<AttributionRelationship> attrib1 = relationshipDAO.lookupAttributionRelationship(roster.getId(), patient1.getID());
            relationshipDAO.refresh(attrib1.get());
            assertTrue(attrib1.get().isInactive());

            Optional<AttributionRelationship> attrib2 = relationshipDAO.lookupAttributionRelationship(roster.getId(), patient2.getID());
            assertTrue(attrib2.isEmpty());

            Optional<AttributionRelationship> attrib3 = relationshipDAO.lookupAttributionRelationship(roster.getId(), patient3.getID());
            relationshipDAO.refresh(attrib3.get());
            assertFalse(attrib3.get().isInactive());
        });
    }
}
