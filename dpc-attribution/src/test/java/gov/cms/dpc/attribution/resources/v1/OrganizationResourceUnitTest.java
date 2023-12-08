package gov.cms.dpc.attribution.resources.v1;

import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.attribution.DPCAttributionConfiguration;
import gov.cms.dpc.attribution.jdbi.EndpointDAO;
import gov.cms.dpc.attribution.jdbi.OrganizationDAO;
import gov.cms.dpc.common.entities.AddressEntity;
import gov.cms.dpc.common.entities.ContactEntity;
import gov.cms.dpc.common.entities.EndpointEntity;
import gov.cms.dpc.common.entities.NameEntity;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class OrganizationResourceUnitTest {

    @Mock
    OrganizationDAO mockOrganizationDao;

    @Mock
    EndpointDAO mockEndpointDao;

    @Mock
    Supplier<UUID> uuidSupplier;

    private DPCAttributionConfiguration configuration;

    private FHIREntityConverter converter = FHIREntityConverter.initialize();

    private OrganizationResource resource;

    private String lookbackExcemptOrgId = "0ab352f1-2bf1-44c4-aa7a-3004a1ffef12";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        configuration = new DPCAttributionConfiguration();
        resource = new OrganizationResource(converter,mockOrganizationDao,mockEndpointDao, configuration);
    }

    @Test
    void testGetOrganizationsByIds() {
        OrganizationEntity orgEnt1 = createOrganizationEntity("123", "org1");
        OrganizationEntity orgEnt2 = createOrganizationEntity("456", "org2");
        List<OrganizationEntity> orgEntList = new ArrayList<>();
        orgEntList.add(orgEnt1);
        orgEntList.add(orgEnt2);
        Mockito.when(mockOrganizationDao.getOrganizationsByIds(any())).thenReturn(orgEntList);

        List<Organization> orgs = resource.searchOrganizations("id|123,456");
        assertEquals(2, orgs.size());
    }

    @Test
    void submitTestOrganizationAndNoLookbackExemptions() {
        Mockito.when(mockOrganizationDao.registerOrganization(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
        configuration.setLookBackExemptOrgs(null);
        final Bundle bundle = buildBundleWithTestOrg(lookbackExcemptOrgId);

        Response response = resource.submitOrganization(bundle);
        Organization orgCreated = (Organization) response.getEntity();
        assertEquals(201, response.getStatus(), "Should have returned a 200 status");
        assertEquals(lookbackExcemptOrgId, orgCreated.getId(), "UUID passed in should have been used");
    }

    @Test
    void submitTestOrganizationWithLookbackExemptions() {
        Mockito.when(mockOrganizationDao.registerOrganization(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
        configuration.setLookBackExemptOrgs(List.of(lookbackExcemptOrgId));
        final Bundle bundle = buildBundleWithTestOrg(lookbackExcemptOrgId);

        Response response = resource.submitOrganization(bundle);
        Organization orgCreated = (Organization) response.getEntity();
        assertEquals(201, response.getStatus(), "Should have returned a 200 status");
        assertEquals(lookbackExcemptOrgId, orgCreated.getId(), "UUID passed in should have been used");
    }

    @Test
    void submitOrganizationWithIdSpecified() {
        Mockito.when(mockOrganizationDao.registerOrganization(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
        configuration.setLookBackExemptOrgs(List.of(lookbackExcemptOrgId));
        UUID uuid = UUID.randomUUID();
        final Bundle bundle = buildBundleWithTestOrg(uuid.toString());

        Response response = resource.submitOrganization(bundle);

        Organization orgCreated = (Organization) response.getEntity();
        assertEquals(201, response.getStatus(), "Should have returned a 200 status");
        assertEquals(uuid.toString(), orgCreated.getId(), "UUID passed in should have been used");
    }

    @Test
    void submitOrganizationWithNoIdSpecified() throws IllegalAccessException, NoSuchFieldException {
        Mockito.when(mockOrganizationDao.registerOrganization(any())).thenAnswer(invocation -> invocation.getArguments()[0]);
        configuration.setLookBackExemptOrgs(List.of(lookbackExcemptOrgId));
        Field supplierField = OrganizationResource.class.getDeclaredField("uuidSupplier");
        supplierField.setAccessible(true);
        supplierField.set(resource, uuidSupplier);
        String validUUID = "df62c0c9-44df-476d-85fc-555c075fbb61"; //simulate valid random UUID that is not exempt;
        Mockito.when(uuidSupplier.get()).thenReturn(UUID.fromString(lookbackExcemptOrgId), UUID.fromString(validUUID)); //Simulate generating a prohibited org id the first time, but not the second.

        final Bundle bundle = buildBundleWithTestOrg(null);

        Response response = resource.submitOrganization(bundle);
        Organization orgCreated = (Organization) response.getEntity();
        assertEquals(201, response.getStatus(), "Should have returned a 200 status");
        assertEquals(validUUID, orgCreated.getId(), "Should have use the UUID that was generated second, first one was prohibited.");
    }


    private Bundle buildBundleWithTestOrg(String uuid){
        Organization organization = AttributionTestHelpers.createOrgResource(uuid, "1334567892");
        return AttributionTestHelpers.createBundle(organization);
    }

    private OrganizationEntity createOrganizationEntity(String orgId, String orgName) {
        AddressEntity addressEntity = new AddressEntity();
        addressEntity.setLine1("123 Test Street");
        NameEntity nameEntity = new NameEntity();
        nameEntity.setFamily("family");
        ContactEntity contactEntity = new ContactEntity();
        contactEntity.setName(nameEntity);
        contactEntity.setAddress(addressEntity);
        contactEntity.setTelecom(List.of());
        EndpointEntity endpointEntity = new EndpointEntity();
        endpointEntity.setId(UUID.randomUUID());
        OrganizationEntity.OrganizationID orgEntId = new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES, orgId);
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(UUID.randomUUID());
        organizationEntity.setOrganizationID(orgEntId);
        organizationEntity.setOrganizationName(orgName);
        organizationEntity.setOrganizationAddress(addressEntity);
        organizationEntity.setContacts(List.of(contactEntity));
        organizationEntity.setEndpoints(List.of(endpointEntity));
        return organizationEntity;
    }
}