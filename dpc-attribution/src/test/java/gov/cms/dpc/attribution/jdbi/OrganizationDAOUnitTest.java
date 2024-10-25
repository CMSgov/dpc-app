package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.AbstractAttributionDAOTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrganizationDAOUnitTest extends AbstractAttributionDAOTest {
	private RelationshipDAO relationshipDAO;
	private PatientDAO patientDAO;
	private OrganizationDAO organizationDAO;
	private ProviderDAO providerDAO;
	private RosterDAO rosterDAO;
	private DPCManagedSessionFactory dpcManagedSessionFactory;

	@BeforeEach
	public void setup() {
		dpcManagedSessionFactory = new DPCManagedSessionFactory(db.getSessionFactory());
		relationshipDAO = new RelationshipDAO(dpcManagedSessionFactory);
		patientDAO = new PatientDAO(dpcManagedSessionFactory);
		organizationDAO = new OrganizationDAO(dpcManagedSessionFactory);
		rosterDAO = new RosterDAO(dpcManagedSessionFactory);
		providerDAO = new ProviderDAO(dpcManagedSessionFactory);
	}

	@Test
	void test_delete_cascades() {
		OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();
		PatientEntity pat = AttributionTestHelpers.createPatientEntity(org);
		ProviderEntity provider = AttributionTestHelpers.createProviderEntity(org);
		RosterEntity roster = AttributionTestHelpers.createRosterEntity(org, provider);
		AttributionRelationship attribution = AttributionTestHelpers.createAttributionRelationship(roster, pat);

		// Insert org, provider, patient, attribution and roster
		db.inTransaction(() -> {
			organizationDAO.registerOrganization(org);
			patientDAO.persistPatient(pat);
			providerDAO.persistProvider(provider);
			rosterDAO.persistEntity(roster);
			relationshipDAO.addAttributionRelationship(attribution);
		});

		// Delete
		db.inTransaction(() -> {
			dpcManagedSessionFactory.getSessionFactory().getCurrentSession().clear();
			OrganizationEntity persistedOrg = organizationDAO.fetchOrganization(org.getId()).get();
			organizationDAO.deleteOrganization(persistedOrg);
		});

		// Test
		assertTrue(organizationDAO.fetchOrganization(org.getId()).isEmpty());
		assertTrue(patientDAO.getPatient(pat.getID()).isEmpty());
		assertTrue(rosterDAO.getEntity(roster.getId()).isEmpty());
		assertTrue(relationshipDAO.lookupAttributionRelationship(roster.getId(), pat.getID()).isEmpty());
	}
}
