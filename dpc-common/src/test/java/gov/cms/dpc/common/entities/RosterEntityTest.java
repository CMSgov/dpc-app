package gov.cms.dpc.common.entities;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import gov.cms.dpc.fhir.DPCIdentifierSystem;

import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Group;
import org.hl7.fhir.dstu3.model.Meta;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class RosterEntityTest {

	@Test
	public void testGettersAndSetters() {
		RosterEntity roster = new RosterEntity();
		UUID id = UUID.randomUUID();
		ProviderEntity provider = new ProviderEntity();
		OrganizationEntity managingOrg = new OrganizationEntity();
		List<AttributionRelationship> attributions = List.of(new AttributionRelationship(),
				new AttributionRelationship());
		OffsetDateTime createdUpdatedAt = OffsetDateTime.now();

		roster.setId(id);
		roster.setAttributedProvider(provider);
		roster.setManagingOrganization(managingOrg);
		roster.setAttributions(attributions);
		roster.setCreatedAt(createdUpdatedAt);
		roster.setUpdatedAt(createdUpdatedAt);

		assertEquals(id, roster.getId());
		assertEquals(provider, roster.getAttributedProvider());
		assertEquals(managingOrg, roster.getManagingOrganization());
		assertEquals(attributions, roster.getAttributions());
		assertEquals(createdUpdatedAt, roster.getCreatedAt());
		assertEquals(createdUpdatedAt, roster.getUpdatedAt());
	}

	@Test
	public void testFromFHIR() {
		Group attributionRoster = new Group();

		Coding coding = new Coding();
		coding.setSystem(DPCIdentifierSystem.DPC.getSystem());
		coding.setCode(UUID.randomUUID().toString());

		Meta meta = new Meta();
		meta.addTag(coding);
		attributionRoster.setMeta(meta);
		attributionRoster.setId(UUID.randomUUID().toString());

		ProviderEntity provider = new ProviderEntity();

		OffsetDateTime expirationDate = OffsetDateTime.now().plusDays(30);

		RosterEntity rosterEntity = RosterEntity.fromFHIR(attributionRoster, provider, expirationDate);
		assertNotNull(rosterEntity);
		assertNotNull(rosterEntity.getManagingOrganization());
		assertEquals(provider, rosterEntity.getAttributedProvider());
		assertEquals(0, rosterEntity.getAttributions().size());

	}

	@Test
	public void testEqualsAndHashCode() {
		UUID id = UUID.randomUUID();
		RosterEntity r1 = new RosterEntity();
		r1.setId(id);
		r1.setAttributedProvider(new ProviderEntity());

		RosterEntity r2 = new RosterEntity();
		r2.setId(id);
		r2.setAttributedProvider(new ProviderEntity());

		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());
	}

}
