package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.AbstractMultipleDAOTest;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.HumanName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PatientDAOUnitTest extends AbstractMultipleDAOTest {
	PatientDAOUnitTest() {
		super(
			PatientEntity.class,
			OrganizationEntity.class,
			AttributionRelationship.class,
			RosterEntity.class,
			ProviderEntity.class,
			ContactEntity.class,
			EndpointEntity.class,
			ContactPointEntity.class
		);
	}

	private PatientDAO patientDAO;
	private OrganizationDAO organizationDAO;

	@BeforeEach
	private void setup() {
		patientDAO = new PatientDAO(new DPCManagedSessionFactory(db.getSessionFactory()));
		organizationDAO = new OrganizationDAO(new DPCManagedSessionFactory(db.getSessionFactory()));
	}

	@Test
	public void test_patientSearch_by_resourceId_happy_path() {
		OrganizationEntity org = createOrganization();
		PatientEntity pat1 = createPatient(org, "1S00EU8FE91");
		PatientEntity pat2 = createPatient(org, "1SQ3F00AA00");
		PatientEntity pat3 = createPatient(org, "5S58A00AA00");

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(org);
		});
		db.inTransaction(() -> {
			patientDAO.persistPatient(pat1);
			patientDAO.persistPatient(pat2);
		});

		List<PatientEntity> patients = patientDAO.patientSearch(List.of(pat1.getID(), pat2.getID()));

		assertEquals(2, patients.size());
		assertTrue(patients.contains(pat1));
		assertTrue(patients.contains(pat2));
		assertFalse(patients.contains(pat3));
	}

	private PatientEntity createPatient(OrganizationEntity org, String mbi) {
		PatientEntity patientEntity = new PatientEntity();
		patientEntity.setID(UUID.randomUUID());
		patientEntity.setBeneficiaryID(mbi);
		patientEntity.setDob(LocalDate.of(1980, 7, 14));
		patientEntity.setGender(Enumerations.AdministrativeGender.MALE);
		patientEntity.setOrganization(org);

		return patientEntity;
	}

	private OrganizationEntity createOrganization() {
		AddressEntity addressEntity = new AddressEntity();
		addressEntity.setType(Address.AddressType.POSTAL);
		addressEntity.setUse(Address.AddressUse.HOME);
		addressEntity.setLine1("123 Test Street");

		NameEntity nameEntity = new NameEntity();
		nameEntity.setGiven("given");
		nameEntity.setFamily("family");
		nameEntity.setUse(HumanName.NameUse.OFFICIAL);

		ContactEntity contactEntity = new ContactEntity();
		contactEntity.setName(nameEntity);
		contactEntity.setAddress(addressEntity);
		contactEntity.setTelecom(List.of());

		EndpointEntity endpointEntity = new EndpointEntity();
		endpointEntity.setId(UUID.randomUUID());
		endpointEntity.setName("endpoint");
		endpointEntity.setStatus(Endpoint.EndpointStatus.ACTIVE);
		endpointEntity.setAddress("http://www.endpoint.com");

		OrganizationEntity.OrganizationID orgEntId = new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES, NPIUtil.generateNPI());
		OrganizationEntity organizationEntity = new OrganizationEntity();
		organizationEntity.setId(UUID.randomUUID());
		organizationEntity.setOrganizationID(orgEntId);
		organizationEntity.setOrganizationName("Name");
		organizationEntity.setOrganizationAddress(addressEntity);
		organizationEntity.setContacts(List.of(contactEntity));
		organizationEntity.setEndpoints(List.of(endpointEntity));

		return organizationEntity;
	}
}
