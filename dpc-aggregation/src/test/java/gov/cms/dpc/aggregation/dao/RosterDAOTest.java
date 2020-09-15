package gov.cms.dpc.aggregation.dao;

import gov.cms.dpc.aggregation.DPCAggregationConfiguration;
import gov.cms.dpc.aggregation.DPCAggregationService;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.IntegrationTest;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@IntegrationTest
@ExtendWith(DropwizardExtensionsSupport.class)
public class RosterDAOTest {

    protected static final DropwizardTestSupport<DPCAggregationConfiguration> APPLICATION = new DropwizardTestSupport<>(DPCAggregationService.class, "ci.application.conf", ConfigOverride.config("server.applicationConnectors[0].port", "7777"));

    @BeforeAll
    static void start() {
        APPLICATION.before();
    }

    @AfterAll
    static void stop() {
        APPLICATION.after();
    }

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(RosterEntity.class)
            .addEntityClass(OrganizationEntity.class)
            .addEntityClass(PatientEntity.class)
            .addEntityClass(AttributionRelationship.class)
            .addEntityClass(ContactEntity.class)
            .addEntityClass(EndpointEntity.class)
            .addEntityClass(ContactPointEntity.class)
            .addEntityClass(AddressEntity.class)
            .addEntityClass(ProviderEntity.class).build();

    private RosterDAO rosterDAO;
    private OrganizationEntity organizationEntity;
    private ProviderEntity providerEntity;
    private PatientEntity patientEntity;
    private RosterEntity rosterEntity;

    @BeforeEach
    public void setupData() {
        Session session = database.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        organizationEntity = organizationEntity(session);
        patientEntity = patientEntity(organizationEntity, session);
        providerEntity = providerEntity(organizationEntity, patientEntity, session);
        transaction.commit();
        session.close();

        Session session2 = database.getSessionFactory().openSession();
        Transaction transaction2 = session2.beginTransaction();
        rosterEntity = rosterEntity(organizationEntity, providerEntity, patientEntity, session2);
        transaction2.commit();
        session2.close();

        rosterDAO = new RosterDAO(new DPCManagedSessionFactory(database.getSessionFactory()));
    }

    @Test
    public void testGettingProviderNPI() {
        Optional<String> npi = rosterDAO.retrieveProviderNPIFromRoster(organizationEntity.getId(), providerEntity.getID(), patientEntity.getBeneficiaryID());
        Assertions.assertTrue(npi.isPresent());
        Assertions.assertEquals(providerEntity.getProviderNPI(), npi.get());

        npi = rosterDAO.retrieveProviderNPIFromRoster(organizationEntity.getId(), rosterEntity.getId(), patientEntity.getBeneficiaryID());
        Assertions.assertTrue(npi.isPresent());
        Assertions.assertEquals(providerEntity.getProviderNPI(), npi.get());

        npi = rosterDAO.retrieveProviderNPIFromRoster(organizationEntity.getId(), UUID.randomUUID(), patientEntity.getBeneficiaryID());
        Assertions.assertTrue(npi.isEmpty());
    }

    private OrganizationEntity organizationEntity(Session session) {
        AddressEntity addressEntity = new AddressEntity();
        addressEntity.setUse(Address.AddressUse.HOME);
        addressEntity.setType(Address.AddressType.PHYSICAL);
        addressEntity.setCity("Plano");
        addressEntity.setLine1("11 Main St");
        addressEntity.setPostalCode("75075");
        addressEntity.setState("TX");
        addressEntity.setCountry("US");
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(UUID.randomUUID());
        organizationEntity.setOrganizationID(new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES, NPIUtil.generateNPI()));
        organizationEntity.setOrganizationName("orgname");
        organizationEntity.setOrganizationAddress(addressEntity);
        session.save(organizationEntity);
        return organizationEntity;
    }

    private PatientEntity patientEntity(OrganizationEntity organizationEntity, Session session) {
        PatientEntity patientEntity = new PatientEntity();
        patientEntity.setID(UUID.randomUUID());
        patientEntity.setGender(Enumerations.AdministrativeGender.MALE);
        patientEntity.setFirstName("Bob");
        patientEntity.setLastName("Jones");
        patientEntity.setDob(LocalDate.of(1985,2,22));
        patientEntity.setBeneficiaryID("3SQ0C00AA00");
        patientEntity.setOrganization(organizationEntity);
        session.save(patientEntity);
        return patientEntity;
    }

    private ProviderEntity providerEntity(OrganizationEntity organizationEntity, PatientEntity patientEntity, Session session) {
        ProviderEntity providerEntity = new ProviderEntity();
        providerEntity.setOrganization(organizationEntity);
        providerEntity.setProviderNPI(NPIUtil.generateNPI());
        providerEntity.setLastName("Smith");
        providerEntity.setFirstName("Greg");
        providerEntity.setID(UUID.randomUUID());
        session.save(providerEntity);
        return providerEntity;
    }

    private RosterEntity rosterEntity(OrganizationEntity organizationEntity, ProviderEntity providerEntity, PatientEntity patientEntity, Session session) {
        RosterEntity rosterEntity = new RosterEntity();
        rosterEntity.setId(UUID.randomUUID());
        rosterEntity.setAttributedProvider(session.load(ProviderEntity.class, providerEntity.getID()));
        rosterEntity.setManagingOrganization(session.load(OrganizationEntity.class, organizationEntity.getId()));

        AttributionRelationship attributionRelationship = new AttributionRelationship(rosterEntity, session.load(PatientEntity.class, patientEntity.getID()));
        attributionRelationship.setAttributionID(1L);
        attributionRelationship.setPeriodBegin(OffsetDateTime.MIN);
        attributionRelationship.setPeriodEnd(OffsetDateTime.MAX);
        session.save(attributionRelationship);

        rosterEntity.setAttributions(List.of(attributionRelationship));
        session.save(rosterEntity);

        return rosterEntity;
    }
}
