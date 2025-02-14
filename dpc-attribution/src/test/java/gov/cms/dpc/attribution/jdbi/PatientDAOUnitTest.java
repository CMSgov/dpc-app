package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.AbstractAttributionDAOTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientDAOUnitTest extends AbstractAttributionDAOTest {
	private PatientDAO patientDAO;
	private OrganizationDAO organizationDAO;

	@BeforeEach
	public void setup() throws Exception {
		DPCManagedSessionFactory dpcManagedSessionFactory = new DPCManagedSessionFactory(db.getSessionFactory());

		// Chunk size is set to 1000 in the current config
		patientDAO = new PatientDAO(dpcManagedSessionFactory, 1000);
		organizationDAO = new OrganizationDAO(dpcManagedSessionFactory);
	}

	@Test
	void test_batch_patientSearchById_happy_path() {
		OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();
		PatientEntity pat1 = AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat2 = AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat3 = AttributionTestHelpers.createPatientEntity(org);

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(org);
			patientDAO.persistPatient(pat1);
			patientDAO.persistPatient(pat2);
			patientDAO.persistPatient(pat3);
		});

		List<PatientEntity> patients = patientDAO.bulkPatientSearchById(org.getId(), List.of(pat1.getID(), pat2.getID()));

		assertEquals(2, patients.size());
		assertTrue(patients.contains(pat1));
		assertTrue(patients.contains(pat2));
		assertFalse(patients.contains(pat3));
	}

	@Test
	void test_batch_patientSearchById_only_finds_correct_org() {
		OrganizationEntity goodOrg = AttributionTestHelpers.createOrganizationEntity();
		OrganizationEntity badOrg = AttributionTestHelpers.createOrganizationEntity();
		PatientEntity pat1 = AttributionTestHelpers.createPatientEntity(goodOrg);
		PatientEntity pat2 = AttributionTestHelpers.createPatientEntity(goodOrg);
		PatientEntity pat3 = AttributionTestHelpers.createPatientEntity(badOrg);

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(goodOrg);
			organizationDAO.registerOrganization(badOrg);
			patientDAO.persistPatient(pat1);
			patientDAO.persistPatient(pat2);
			patientDAO.persistPatient(pat3);
		});

		List<PatientEntity> patients = patientDAO.bulkPatientSearchById(goodOrg.getId(), List.of(pat1.getID(), pat2.getID(), pat3.getID()));

		assertEquals(2, patients.size());
		assertTrue(patients.contains(pat1));
		assertTrue(patients.contains(pat2));
		assertFalse(patients.contains(pat3));
	}

	@Test
	void test_batch_patientSearchByMbi_happy_path() {
		OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();
		PatientEntity pat1 = AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat2 = AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat3 = AttributionTestHelpers.createPatientEntity(org);

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(org);
			patientDAO.persistPatient(pat1);
			patientDAO.persistPatient(pat2);
			patientDAO.persistPatient(pat3);
		});

		List<PatientEntity> patients = patientDAO.bulkPatientSearchByMbi(org.getId(), List.of(pat1.getBeneficiaryID(), pat2.getBeneficiaryID()));

		assertEquals(2, patients.size());
		assertTrue(patients.contains(pat1));
		assertTrue(patients.contains(pat2));
		assertFalse(patients.contains(pat3));
	}

	@Test
	void test_batch_patientSearchByMbi_only_finds_correct_org() {
		OrganizationEntity goodOrg = AttributionTestHelpers.createOrganizationEntity();
		OrganizationEntity badOrg = AttributionTestHelpers.createOrganizationEntity();
		PatientEntity pat1 = AttributionTestHelpers.createPatientEntity(goodOrg);
		PatientEntity pat2 = AttributionTestHelpers.createPatientEntity(goodOrg);
		PatientEntity pat3 = AttributionTestHelpers.createPatientEntity(badOrg);

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(goodOrg);
			organizationDAO.registerOrganization(badOrg);
			patientDAO.persistPatient(pat1);
			patientDAO.persistPatient(pat2);
			patientDAO.persistPatient(pat3);
		});

		List<PatientEntity> patients = patientDAO.bulkPatientSearchByMbi(goodOrg.getId(), List.of(
			pat1.getBeneficiaryID(), pat2.getBeneficiaryID(), pat3.getBeneficiaryID())
		);

		assertEquals(2, patients.size());
		assertTrue(patients.contains(pat1));
		assertTrue(patients.contains(pat2));
		assertFalse(patients.contains(pat3));
	}

	@Test
	void test_bulkPatientSearchByMbi_handles_large_requests() throws IOException {
		OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();
		PatientEntity patientEntity = AttributionTestHelpers.createPatientEntity(org);

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(org);
			patientDAO.persistPatient(patientEntity);
		});

		List<String> mbis = new ArrayList<>();
		mbis.add(patientEntity.getBeneficiaryID());

		// Add enough fake MBIs to make sure we have to chunk the query
		for(int i=0; i< 50000; i++){
			mbis.add("fake_mbi");
		}

		List<PatientEntity> patients = patientDAO.bulkPatientSearchByMbi(org.getId(), mbis);

		assertEquals(1, patients.size());
		assertTrue(patients.contains(patientEntity));
	}
}
