package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.AbstractAttributionDAOTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.common.entities.PatientEntity;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PatientDAOUnitTest extends AbstractAttributionDAOTest {
	private PatientDAO patientDAO;
	private OrganizationDAO organizationDAO;

	@BeforeEach
	public void setup() {
		DPCManagedSessionFactory dpcManagedSessionFactory = new DPCManagedSessionFactory(db.getSessionFactory());
		patientDAO = new PatientDAO(dpcManagedSessionFactory);
		organizationDAO = new OrganizationDAO(dpcManagedSessionFactory);
	}

	@Test
	public void test_patientSearch_by_resourceId_happy_path() {
		OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();
		PatientEntity pat1 = AttributionTestHelpers.createPatientEntity(org, "1S00EU8FE91");
		PatientEntity pat2 = AttributionTestHelpers.createPatientEntity(org, "1SQ3F00AA00");
		PatientEntity pat3 = AttributionTestHelpers.createPatientEntity(org, "5S58A00AA00");

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(org);
			patientDAO.persistPatient(pat1);
			patientDAO.persistPatient(pat2);
			patientDAO.persistPatient(pat3);
		});

		List<PatientEntity> patients = patientDAO.patientSearch(org.getId(), List.of(pat1.getID(), pat2.getID()));

		assertEquals(2, patients.size());
		assertTrue(patients.contains(pat1));
		assertTrue(patients.contains(pat2));
		assertFalse(patients.contains(pat3));
	}
}
