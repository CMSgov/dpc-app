package gov.cms.dpc.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class AttributionRelationshipTest {

	@Test
	public void testEqualsAndHashCode() {
		AttributionRelationship r1 = new AttributionRelationship();
		AttributionRelationship r2 = new AttributionRelationship();

		//Objects should be equal at this point.
		assertEquals(r1, r2);
		assertEquals(r1.hashCode(), r2.hashCode());

		RosterEntity roster = new RosterEntity();
		PatientEntity patient = new PatientEntity();

		OffsetDateTime periodBegin = OffsetDateTime.now();
		OffsetDateTime periodEnd = periodBegin.plusDays(30);

		r1.setRoster(roster);
		r1.setPatient(patient);
		r1.setPeriodBegin(periodBegin);
		r1.setPeriodEnd(periodEnd);

		assertNotEquals(r1, r2);

		r2.setRoster(roster);
		r2.setPatient(patient);
		r2.setPeriodBegin(periodBegin);
		r2.setPeriodEnd(periodEnd);

		assertEquals(r1,r2);
		assertEquals(r1.hashCode(), r2.hashCode());


	}

	@Test
	public void testToString() {
		AttributionRelationship relationship = new AttributionRelationship();
		relationship.setAttributionID(1L);
		RosterEntity roster = new RosterEntity();
		relationship.setRoster(new RosterEntity());
		PatientEntity patient = new PatientEntity();
		relationship.setPatient(patient);
		relationship.setInactive(true);
		
		OffsetDateTime periodBegin = OffsetDateTime.now();
		OffsetDateTime periodEnd = periodBegin.plusDays(30);

		relationship.setPeriodBegin(periodBegin);
		relationship.setPeriodEnd(periodEnd);

		String expected = "AttributionRelationship{attributionID=1, roster=" + roster.toString() + ", patient=" + patient.toString() + ", inactive=true, begin=" + periodBegin + ", end="+ periodEnd + "}";
		assertEquals(expected, relationship.toString());
	}

	@Test
	public void testConstructors() {
		RosterEntity roster = new RosterEntity();
		PatientEntity patient = new PatientEntity();
		Timestamp created = Timestamp.from(Instant.now());

		AttributionRelationship r1 = new AttributionRelationship(roster, patient);
		assertEquals(roster, r1.getRoster());
		assertEquals(patient, r1.getPatient());

		AttributionRelationship r2 = new AttributionRelationship(roster, patient, created);
		assertEquals(roster, r2.getRoster());
		assertEquals(patient, r2.getPatient());
		assertEquals(OffsetDateTime.ofInstant(created.toInstant(), ZoneOffset.UTC), r2.getPeriodBegin());
	}

}
