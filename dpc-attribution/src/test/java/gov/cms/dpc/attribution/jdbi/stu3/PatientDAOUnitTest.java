package gov.cms.dpc.attribution.jdbi.stu3;

import gov.cms.dpc.attribution.jdbi.PageResult;
import gov.cms.dpc.attribution.jdbi.PatientSearchQuery;

import gov.cms.dpc.attribution.Stu3AbstractAttributionDAOTest;
import gov.cms.dpc.attribution.Stu3AttributionTestHelpers;
import gov.cms.dpc.common.entities.stu3.OrganizationEntity;
import gov.cms.dpc.common.entities.stu3.PatientEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.fhir.stu3.FHIRExtractors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientDAOUnitTest extends Stu3AbstractAttributionDAOTest {
	private PatientDAO patientDAO;
	private OrganizationDAO organizationDAO;

	@BeforeEach
	void setup() {
		DPCManagedSessionFactory dpcManagedSessionFactory = new DPCManagedSessionFactory(db.getSessionFactory());

		// Chunk size is set to 1000 in the current config
		patientDAO = new PatientDAO(dpcManagedSessionFactory, 1000);
		organizationDAO = new OrganizationDAO(dpcManagedSessionFactory);
	}

	@Test
	void test_batch_patientSearchById_happy_path() {
		OrganizationEntity org = Stu3AttributionTestHelpers.createOrganizationEntity();
		PatientEntity pat1 = Stu3AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat2 = Stu3AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat3 = Stu3AttributionTestHelpers.createPatientEntity(org);

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
		OrganizationEntity goodOrg = Stu3AttributionTestHelpers.createOrganizationEntity();
		OrganizationEntity badOrg = Stu3AttributionTestHelpers.createOrganizationEntity();
		PatientEntity pat1 = Stu3AttributionTestHelpers.createPatientEntity(goodOrg);
		PatientEntity pat2 = Stu3AttributionTestHelpers.createPatientEntity(goodOrg);
		PatientEntity pat3 = Stu3AttributionTestHelpers.createPatientEntity(badOrg);

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
		OrganizationEntity org = Stu3AttributionTestHelpers.createOrganizationEntity();
		PatientEntity pat1 = Stu3AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat2 = Stu3AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat3 = Stu3AttributionTestHelpers.createPatientEntity(org);

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
		OrganizationEntity goodOrg = Stu3AttributionTestHelpers.createOrganizationEntity();
		OrganizationEntity badOrg = Stu3AttributionTestHelpers.createOrganizationEntity();
		PatientEntity pat1 = Stu3AttributionTestHelpers.createPatientEntity(goodOrg);
		PatientEntity pat2 = Stu3AttributionTestHelpers.createPatientEntity(goodOrg);
		PatientEntity pat3 = Stu3AttributionTestHelpers.createPatientEntity(badOrg);

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
		OrganizationEntity org = Stu3AttributionTestHelpers.createOrganizationEntity();
		PatientEntity patientEntity = Stu3AttributionTestHelpers.createPatientEntity(org);

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

    @Test
    void test_countPatients() {
        OrganizationEntity org = Stu3AttributionTestHelpers.createOrganizationEntity();
        String orgRef = "Organization/" + org.getId();
        PatientEntity patient1 = Stu3AttributionTestHelpers.createPatientEntity(org);
        PatientEntity patient2 = Stu3AttributionTestHelpers.createPatientEntity(org);
        PatientEntity patient3 = Stu3AttributionTestHelpers.createPatientEntity(org);
        PatientEntity patient4 = Stu3AttributionTestHelpers.createPatientEntity(org);
        PatientEntity patient5 = Stu3AttributionTestHelpers.createPatientEntity(org);

        db.inTransaction(() -> {
            organizationDAO.registerOrganization(org);
            patientDAO.persistPatient(patient1);
            patientDAO.persistPatient(patient2);
            patientDAO.persistPatient(patient3);
            patientDAO.persistPatient(patient4);
            patientDAO.persistPatient(patient5);
        });

        PatientSearchQuery daoSearchQuery = new PatientSearchQuery();
        daoSearchQuery.setOrganizationID(FHIRExtractors.getEntityUUID(orgRef));
        daoSearchQuery.setCount(0);
        int patientCount = patientDAO.countMatchingPatients(daoSearchQuery);
        assertEquals(5, patientCount);
    }

    @Test
    void test_patientSearch() {
        OrganizationEntity org = Stu3AttributionTestHelpers.createOrganizationEntity();
        String orgRef = "Organization/" + org.getId();
        PatientEntity patient1 = Stu3AttributionTestHelpers.createPatientEntity(org);
        PatientEntity patient2 = Stu3AttributionTestHelpers.createPatientEntity(org);
        PatientEntity patient3 = Stu3AttributionTestHelpers.createPatientEntity(org);
        PatientEntity patient4 = Stu3AttributionTestHelpers.createPatientEntity(org);
        PatientEntity patient5 = Stu3AttributionTestHelpers.createPatientEntity(org);

        db.inTransaction(() -> {
            organizationDAO.registerOrganization(org);
            patientDAO.persistPatient(patient1);
            patientDAO.persistPatient(patient2);
            patientDAO.persistPatient(patient3);
            patientDAO.persistPatient(patient4);
            patientDAO.persistPatient(patient5);
        });
        PatientSearchQuery daoSearchQuery = new PatientSearchQuery();
        daoSearchQuery.setOrganizationID(FHIRExtractors.getEntityUUID(orgRef));

        daoSearchQuery.setCount(2);
        daoSearchQuery.setPageOffset(0);
        PageResult<PatientEntity> firstPageResults = patientDAO.patientSearch(daoSearchQuery);
        assertEquals(2, firstPageResults.getResults().size());
        assertTrue(firstPageResults.hasNext());

        daoSearchQuery.setPageOffset(2);
        PageResult<PatientEntity> secondPageResults = patientDAO.patientSearch(daoSearchQuery);
        assertEquals(2, secondPageResults.getResults().size());
        assertTrue(secondPageResults.hasNext());

        daoSearchQuery.setPageOffset(4);
        PageResult<PatientEntity> thirdPageResults = patientDAO.patientSearch(daoSearchQuery);
        assertEquals(1, thirdPageResults.getResults().size());
        assertFalse(thirdPageResults.hasNext());
    }
}
