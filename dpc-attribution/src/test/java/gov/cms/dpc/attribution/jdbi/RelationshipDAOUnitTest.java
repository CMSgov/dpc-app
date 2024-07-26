package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.AbstractAttributionDAOTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelationshipDAOUnitTest extends AbstractAttributionDAOTest {
	private RelationshipDAO relationshipDAO;
	private PatientDAO patientDAO;
	private OrganizationDAO organizationDAO;
	private ProviderDAO providerDAO;
	private RosterDAO rosterDAO;

	@BeforeEach
	public void setup() {
		DPCManagedSessionFactory dpcManagedSessionFactory = new DPCManagedSessionFactory(db.getSessionFactory());
		relationshipDAO = new RelationshipDAO(dpcManagedSessionFactory);
		patientDAO = new PatientDAO(dpcManagedSessionFactory);
		organizationDAO = new OrganizationDAO(dpcManagedSessionFactory);
		providerDAO = new ProviderDAO(dpcManagedSessionFactory);
		rosterDAO = new RosterDAO(dpcManagedSessionFactory);
	}

	@Test
	public void test_AttributionRelationship_search_by_roster_and_patient_happy_path() {
		OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();

		PatientEntity pat1 = AttributionTestHelpers.createPatientEntity(org, "1S00EU8FE91");
		PatientEntity pat2 = AttributionTestHelpers.createPatientEntity(org, "1SQ3F00AA00");
		PatientEntity pat3 = AttributionTestHelpers.createPatientEntity(org, "5S58A00AA00");

		ProviderEntity provider = AttributionTestHelpers.createProviderEntity(org);

		RosterEntity roster = AttributionTestHelpers.createRosterEntity(org, provider);

		AttributionRelationship attribution1 = AttributionTestHelpers.createAttributionRelationship(roster, pat1);
		AttributionRelationship attribution2 = AttributionTestHelpers.createAttributionRelationship(roster, pat2);
		AttributionRelationship attribution3 = AttributionTestHelpers.createAttributionRelationship(roster, pat3);

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(org);

			patientDAO.persistPatient(pat1);
			patientDAO.persistPatient(pat2);
			patientDAO.persistPatient(pat3);

			providerDAO.persistProvider(provider);

			rosterDAO.persistEntity(roster);

			relationshipDAO.addAttributionRelationship(attribution1);
			relationshipDAO.addAttributionRelationship(attribution2);
			relationshipDAO.addAttributionRelationship(attribution3);
		});

		List<AttributionRelationship> attributions = relationshipDAO.lookupAttributionRelationships(roster.getId(), List.of(pat1.getID(), pat2.getID()));
		assertEquals(2, attributions.size());
		assertTrue(attributions.contains(attribution1));
		assertTrue(attributions.contains(attribution2));
		assertFalse(attributions.contains(attribution3));
	}
}
