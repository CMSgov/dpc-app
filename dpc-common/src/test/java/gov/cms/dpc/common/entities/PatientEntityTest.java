package gov.cms.dpc.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PatientEntityTest {

	@Test
	public void testGettersAndSetters() {
		PatientEntity patient = new PatientEntity();
		String beneficiaryId = "12345";
		String mbiHash = "54321";
		LocalDate dob = LocalDate.of(1995,1,1);
		AdministrativeGender gender = AdministrativeGender.MALE;
		OrganizationEntity org = new OrganizationEntity();
		List<AttributionRelationship> attributionRelationships = new ArrayList<>();

		patient.setBeneficiaryID(beneficiaryId);
		patient.setMbiHash(mbiHash);
		patient.setDob(dob);
		patient.setGender(gender);
		patient.setOrganization(org);
		patient.setAttributions(attributionRelationships);

		assertEquals(beneficiaryId, patient.getBeneficiaryID());
		assertEquals(mbiHash, patient.getMbiHash());
		assertEquals(dob, patient.getDob());
		assertEquals(gender, patient.getGender());
		assertEquals(org, patient.getOrganization());
		assertEquals(attributionRelationships, patient.getAttributions());

	}

	@Test
	public void testUpdate() {
		PatientEntity patient = new PatientEntity();
		PatientEntity newPatient = new PatientEntity();
		UUID id = UUID.randomUUID();
		newPatient.setFirstName(id.toString());

		patient.update(newPatient);

		assertEquals(newPatient.getFirstName(), patient.getFirstName());
	}

	@Test
	public void testUpperCaseBeneId() {
		PatientEntity patient = new PatientEntity();
		String beneficiaryId = "abcd1234";
		patient.setBeneficiaryID(beneficiaryId);
		patient.upperCaseBeneficiaryId();

		assertEquals("ABCD1234", patient.getBeneficiaryID());
	}

	@Test
	public void testEqualsAndHashCode() {

		PatientEntity p1 = new PatientEntity();
		PatientEntity p2 = new PatientEntity();
		p1.setBeneficiaryID("abcd1234");
		p2.setBeneficiaryID("abcd1234");

		assertTrue(p1.equals(p2));
		assertEquals(p1.hashCode(), p2.hashCode());
	}

	@Test
	public void testLocalDateFunctions() {
		LocalDate localDate = LocalDate.of(2023,10,15);
		Date utilityDate = PatientEntity.fromLocalDate(localDate);
		LocalDate convertedDate = PatientEntity.toLocalDate(utilityDate);

		assertNotNull(convertedDate);
		assertNotNull(utilityDate);
		assertEquals(localDate,convertedDate);
	}
}
