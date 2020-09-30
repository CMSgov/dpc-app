package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.common.FeatureFlagCodes;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.testing.junit5.DAOTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.cfg.AvailableSettings;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
class OrganizationDAOTest extends AbstractAttributionTest{

    public DAOTestExtension daoTestExtension;

    public OrganizationDAOTest(){
       DataSourceFactory dataSourceFactory =  APPLICATION.getConfiguration().getDatabase();
       this.daoTestExtension = DAOTestExtension.newBuilder()
                .addEntityClass(AddressEntity.class)
                .addEntityClass(AttributionRelationship.class)
                .addEntityClass(ContactEntity.class)
                .addEntityClass(ContactPointEntity.class)
                .addEntityClass(EndpointEntity.class)
                .addEntityClass(NameEntity.class)
                .addEntityClass(OrganizationEntity.class)
                .addEntityClass(PatientEntity.class)
                .addEntityClass(PersonEntity.class)
                .addEntityClass(ProviderEntity.class)
                .addEntityClass(RosterEntity.class)
                .setDriver(org.postgresql.Driver.class)
                .setUrl(dataSourceFactory.getUrl())
                .setUsername(dataSourceFactory.getUser())
                .setProperty(AvailableSettings.PASS,dataSourceFactory.getPassword())
                .build();
    }



    private OrganizationDAO organizationDAO;

    @BeforeEach
    public void setUp() throws Throwable {

        organizationDAO = new OrganizationDAO(new DPCManagedSessionFactory(daoTestExtension.getSessionFactory()));
        daoTestExtension.before();
    }

    @Test
    void registerOrganization() {
        OrganizationEntity org = buildValidOrgEntity(UUID.randomUUID(), "Test Org 1", "1334567892");
        OrganizationEntity persistedOrg = daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(org));
        assertNotNull(persistedOrg, "Result should not be null");

        Optional<OrganizationEntity> retrievedOrg = daoTestExtension.inTransaction(()->organizationDAO.fetchOrganization(org.getId()));
        assertTrue(retrievedOrg.isPresent(), "Org should exist.");
        assertEquals("Test Org 1",retrievedOrg.get().getOrganizationName());
        assertEquals("1334567892", retrievedOrg.get().getOrganizationID().getValue());
    }

    @Test
    void registerOrganizationWithFeatureFlags() {
        OrganizationEntity org = buildValidOrgEntity(UUID.randomUUID(), "Test Org 1", "1334567892");
        org.setFeatures(new FeatureFlags());
        org.getFeatures().setFeature(FeatureFlagCodes.ALLOW_ADMIN_HEADERS, true);
        org.getFeatures().setFeature(FeatureFlagCodes.LOOKBACK_MONTHS, 10);
        daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(org));

        Optional<OrganizationEntity> retrievedOrg = daoTestExtension.inTransaction(()->organizationDAO.fetchOrganization(org.getId()));
        assertTrue(retrievedOrg.isPresent(), "Org should exist.");
        assertNotNull(retrievedOrg.get().getFeatures(),"features should not be null");
        assertTrue(retrievedOrg.get().getFeatures().getBooleanFeature(FeatureFlagCodes.ALLOW_ADMIN_HEADERS).isPresent(), "flag should have been present");
        assertTrue(retrievedOrg.get().getFeatures().getBooleanFeature(FeatureFlagCodes.ALLOW_ADMIN_HEADERS).get(), "flag should have been true");
        assertTrue(retrievedOrg.get().getFeatures().getBooleanFeature(FeatureFlagCodes.LOOKBACK_MONTHS).isPresent(), "flag should have been present");
        assertEquals(10, retrievedOrg.get().getFeatures().getIntegerFeature(FeatureFlagCodes.LOOKBACK_MONTHS).get(), "string flag should have been present and equal");
    }

    @Test
    void updateFeatureFlag() {
        OrganizationEntity org = buildValidOrgEntity(UUID.randomUUID(), "Test Org 1", "1334567892");
        org.setFeatures(new FeatureFlags());
        org.getFeatures().setFeature(FeatureFlagCodes.LOOKBACK_MONTHS, 10);
        daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(org));

        org.getFeatures().setFeature(FeatureFlagCodes.LOOKBACK_MONTHS, 42);
        daoTestExtension.inTransaction(()->organizationDAO.updateOrganization(org.getId(), org));

        Optional<OrganizationEntity> retrievedOrg = daoTestExtension.inTransaction(()->organizationDAO.fetchOrganization(org.getId()));
        assertTrue(retrievedOrg.get().getFeatures().getBooleanFeature(FeatureFlagCodes.LOOKBACK_MONTHS).isPresent(), "flag should have been present");
        assertEquals(42, retrievedOrg.get().getFeatures().getIntegerFeature(FeatureFlagCodes.LOOKBACK_MONTHS).get(), "Value was not update");
    }

    @Test
    void fetchOrganization() {
        OrganizationEntity org = buildValidOrgEntity(UUID.randomUUID(), "Test Org 1", "1334567892");
        OrganizationEntity persistedOrg = daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(org));
        assertNotNull(persistedOrg, "Result should not be null");

        Optional<OrganizationEntity> retrievedOrg = daoTestExtension.inTransaction(()->organizationDAO.fetchOrganization(org.getId()));
        assertTrue(retrievedOrg.isPresent(), "Org should exist.");
        assertEquals("Test Org 1",retrievedOrg.get().getOrganizationName());
        assertEquals("1334567892", retrievedOrg.get().getOrganizationID().getValue());

        retrievedOrg = daoTestExtension.inTransaction(()->organizationDAO.fetchOrganization(UUID.randomUUID()));
        assertTrue(retrievedOrg.isEmpty(), "Should not have fetched anything, random UUID was provided.");
    }

    @Test
    void listOrganizations() {
        List<OrganizationEntity> organizationEntities = daoTestExtension.inTransaction(() -> organizationDAO.listOrganizations());
        assertNotNull(organizationEntities,"Returned list should not have been null");
        assertTrue(organizationEntities.isEmpty(),"Returned list should be empty. (Parent class should clean db before each test execution)");

        daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(buildValidOrgEntity(UUID.randomUUID(), "Test Org 1", "1334567892")));
        daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(buildValidOrgEntity(UUID.randomUUID(), "Test Org 2", "1334567892")));
        daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(buildValidOrgEntity(UUID.randomUUID(), "Test Org 3", "1334567892")));
        daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(buildValidOrgEntity(UUID.randomUUID(), "Test Org 4", "1334567892")));

        organizationEntities = daoTestExtension.inTransaction(() -> organizationDAO.listOrganizations());
        assertNotNull(organizationEntities,"Returned list should not have been null");
        assertTrue(organizationEntities.size()==4,"Returned list should have contained 4 organizations");
    }

    @Test
    void updateOrganization() {
        OrganizationEntity org = buildValidOrgEntity(UUID.randomUUID(), "Test Org 1", "1334567892");
        OrganizationEntity persistedOrg = daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(org));
        assertNotNull(persistedOrg, "Result should not be null");

        org.setOrganizationName("New Org Name");
        OrganizationEntity updatedOrg = daoTestExtension.inTransaction(() -> organizationDAO.updateOrganization(org.getId(),org));
        assertNotNull(updatedOrg, "result should not be null");

        Optional<OrganizationEntity> retrievedOrg = daoTestExtension.inTransaction(()->organizationDAO.fetchOrganization(org.getId()));
        assertTrue(retrievedOrg.isPresent(), "Should have fetched updated org");
        assertEquals("New Org Name", retrievedOrg.get().getOrganizationName());
    }

    @Test
    void deleteOrganization() {
        OrganizationEntity org = buildValidOrgEntity(UUID.randomUUID(), "Test Org 1", "1334567892");
        OrganizationEntity persistedOrg = daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(org));
        assertNotNull(persistedOrg, "Result should not be null");

        Optional<OrganizationEntity> retrievedOrg = daoTestExtension.inTransaction(()->organizationDAO.fetchOrganization(org.getId()));
        assertTrue(retrievedOrg.isPresent(), "Should have found newly inserted org");

        daoTestExtension.inTransaction(() -> organizationDAO.deleteOrganization(org));

        retrievedOrg = daoTestExtension.inTransaction(()->organizationDAO.fetchOrganization(org.getId()));
        assertTrue(retrievedOrg.isEmpty(), "Should not have found deleted org");
    }


    @Test
    void searchByIdentifier() {
        daoTestExtension.inTransaction(() -> organizationDAO.registerOrganization(buildValidOrgEntity(UUID.randomUUID(), "Test Org 1", "1334567892")));

        List<OrganizationEntity> organizationEntities = daoTestExtension.inTransaction(() -> organizationDAO.searchByIdentifier("1334567892"));
        assertTrue(organizationEntities.size() == 1, "query should have only returned one org.");

        organizationEntities = daoTestExtension.inTransaction(() -> organizationDAO.searchByIdentifier("non-existing-identifier"));
        assertTrue(organizationEntities.size() == 0, "query should have only returned one org.");
    }

    private OrganizationEntity buildValidOrgEntity(UUID uuid, String name, String npi){
        OrganizationEntity org = new OrganizationEntity();
        org.setId(uuid);
        org.setOrganizationName(name);
        org.setOrganizationID(new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES,npi));
        return org;
    }
}