package gov.cms.dpc.common.entities;

import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PatientEntityTest {

	@Test
	public void testGettersAndSetters() {
		PatientEntity patient = new PatientEntity();
		String beneficiaryId = "12345";
		String mbiHash = "54321";
		LocalDate dob = LocalDate.of(1995, 1, 1);
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

		assertEquals(p1, p2);
		assertEquals(p1.hashCode(), p2.hashCode());
	}

	@Test
	public void testLocalDateFunctions() {
		LocalDate localDate = LocalDate.of(2023, 10, 15);
		Date utilityDate = PatientEntity.fromLocalDate(localDate);
		LocalDate convertedDate = PatientEntity.toLocalDate(utilityDate);

		assertNotNull(convertedDate);
		assertNotNull(utilityDate);
		assertEquals(localDate, convertedDate);
	}

	@Test
	public void testToLocalDateHandlesTimeZoneChange() {
		// A previous version of toLocalDate failed when converting a date/time close to midnight because it converted
		// to UTC before getting the LocalDate.  Make sure that's no longer the case.

		// 1/1/2024 11:55pm EST
		Date testDate = new Calendar.Builder()
			.setDate(2024, 0, 1)
			.setTimeOfDay(23, 55, 0).build().getTime();

		LocalDate resultLocalDate = PatientEntity.toLocalDate(testDate);

		assertEquals(LocalDate.of(2024, 1, 1), resultLocalDate);
	}
}
