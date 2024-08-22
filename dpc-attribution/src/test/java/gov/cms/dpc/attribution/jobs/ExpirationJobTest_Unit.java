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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void testDoJob() throws NoSuchFieldException, SQLException {
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

        List<OrganizationEntity> orgs = organizationDAO.listOrganizations();
        assertEquals(1, orgs.size());
        Optional<AttributionRelationship> attrib = relationshipDAO.lookupAttributionRelationship(roster.getId(), patient.getID());
        assertEquals(false, attrib.isEmpty());
        this.expireAttributions.doJob(this.jobContext);
    }
}
