package gov.cms.dpc.common.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;

public class ProviderEntityTest {

	@Test
	public void testGettersAndSetters() {
		ProviderEntity provider = new ProviderEntity();
		String providerNPI = "1234567890";
		UUID id = UUID.randomUUID();
		OrganizationEntity org = new OrganizationEntity();
		provider.setProviderNPI(providerNPI);
		provider.setID(id);
		provider.setOrganization(org);

		assertEquals(providerNPI, provider.getProviderNPI());
		assertEquals(id, provider.getID());
		assertEquals(org, provider.getOrganization());
	}

	@Test
	public void testAttributedPatients() {
		ProviderEntity provider = new ProviderEntity();
		PatientEntity p1 = new PatientEntity();
		PatientEntity p2 = new PatientEntity();
		provider.setAttributedPatients(List.of(p1, p2));

		assertEquals(2, provider.getAttributedPatients().size());
		assertEquals(p1, provider.getAttributedPatients().get(0));
		assertEquals(p2, provider.getAttributedPatients().get(1));
	}

	@Test
	public void testAttributionRosters() {
		ProviderEntity provider = new ProviderEntity();
		RosterEntity r1 = new RosterEntity();
		RosterEntity r2 = new RosterEntity();
		provider.setAttributionRosters(List.of(r1, r2));

		assertEquals(2, provider.getAttributionRosters().size());
		assertEquals(r1, provider.getAttributionRosters().get(0));
		assertEquals(r2, provider.getAttributionRosters().get(1));
	}

	@Test
	public void testUpdate() {
		ProviderEntity provider = new ProviderEntity();
		String firstName = "Sydney";
		String lastName = "Danger";
		ProviderEntity entity = new ProviderEntity();
		entity.setFirstName(firstName);
		entity.setLastName(lastName);

		provider.update(entity);

		assertEquals(firstName, provider.getFirstName());
		assertEquals(lastName, provider.getLastName());

	}

	@Test
	public void testEqualsAndHashCode() {
		ProviderEntity p1 = new ProviderEntity();
		UUID id = UUID.randomUUID();
		p1.setID(id);
		p1.setProviderNPI("1234567890");

		ProviderEntity p2 = new ProviderEntity();
		assertNotEquals(p2.hashCode(), p1.hashCode());
		p2.setID(id);
		p2.setProviderNPI("1234567890");

		assertEquals(p1, p2);
		assertEquals(p1.hashCode(), p2.hashCode());
	}

}
