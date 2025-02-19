package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.AbstractAttributionDAOTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.ProviderEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProviderDAOUnitTest extends AbstractAttributionDAOTest {
	private ProviderDAO providerDAO;
	private OrganizationDAO organizationDAO;

	@BeforeEach
	public void setup() {
		DPCManagedSessionFactory dpcManagedSessionFactory = new DPCManagedSessionFactory(db.getSessionFactory());
		providerDAO = new ProviderDAO(dpcManagedSessionFactory);
		organizationDAO = new OrganizationDAO(dpcManagedSessionFactory);
	}

	@Test
	void test_bulkProviderSearch() {
		OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();
		ProviderEntity provider1 = AttributionTestHelpers.createProviderEntity(org);
		ProviderEntity provider2 = AttributionTestHelpers.createProviderEntity(org);
		ProviderEntity provider3 = AttributionTestHelpers.createProviderEntity(org);

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(org);
			providerDAO.persistProvider(provider1);
			providerDAO.persistProvider(provider2);
			providerDAO.persistProvider(provider3);
		});

		List<ProviderEntity> providers = providerDAO.bulkProviderSearch(org.getId(), List.of(provider1.getProviderNPI(), provider2.getProviderNPI()));
		assertEquals(2, providers.size());
		assertTrue(providers.contains(provider1));
		assertTrue(providers.contains(provider2));
		assertFalse(providers.contains(provider3));
	}

	@Test
	void test_bulkProviderSearch_only_finds_correct_org() {
		OrganizationEntity goodOrg = AttributionTestHelpers.createOrganizationEntity();
		OrganizationEntity badOrg = AttributionTestHelpers.createOrganizationEntity();
		ProviderEntity provider1 = AttributionTestHelpers.createProviderEntity(goodOrg);
		ProviderEntity provider2 = AttributionTestHelpers.createProviderEntity(goodOrg);
		ProviderEntity provider3 = AttributionTestHelpers.createProviderEntity(badOrg);

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(goodOrg);
			organizationDAO.registerOrganization(badOrg);
			providerDAO.persistProvider(provider1);
			providerDAO.persistProvider(provider2);
			providerDAO.persistProvider(provider3);
		});

		List<ProviderEntity> providers = providerDAO.bulkProviderSearch(goodOrg.getId(), List.of(
			provider1.getProviderNPI(), provider2.getProviderNPI(), provider3.getProviderNPI()
		));
		assertEquals(2, providers.size());
		assertTrue(providers.contains(provider1));
		assertTrue(providers.contains(provider2));
		assertFalse(providers.contains(provider3));
	}
}
