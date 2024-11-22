package gov.cms.dpc.attribution.jdbi;

import gov.cms.dpc.attribution.AbstractAttributionDAOTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.hibernate.attribution.DPCManagedSessionFactory;
import jakarta.persistence.SequenceGenerator;
import org.hibernate.Session;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Relationship data operations")
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
        @DisplayName("Batch search for attributions 🥳")
	public void test_AttributionRelationship_batch_search_happy_path() {
		OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();

		PatientEntity pat1 = AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat2 = AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat3 = AttributionTestHelpers.createPatientEntity(org);

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

	@Test
        @DisplayName("Search attributions with roster filtering 🥳")
	public void test_AttributionRelationship_batch_search_only_finds_correct_roster() {
		OrganizationEntity org = AttributionTestHelpers.createOrganizationEntity();

		PatientEntity pat1 = AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat2 = AttributionTestHelpers.createPatientEntity(org);
		PatientEntity pat3 = AttributionTestHelpers.createPatientEntity(org);

		ProviderEntity provider = AttributionTestHelpers.createProviderEntity(org);

		RosterEntity goodRoster = AttributionTestHelpers.createRosterEntity(org, provider);
		RosterEntity badRoster = AttributionTestHelpers.createRosterEntity(org, provider);

		AttributionRelationship attribution1 = AttributionTestHelpers.createAttributionRelationship(goodRoster, pat1);
		AttributionRelationship attribution2 = AttributionTestHelpers.createAttributionRelationship(goodRoster, pat2);
		AttributionRelationship attribution3 = AttributionTestHelpers.createAttributionRelationship(badRoster, pat3);

		db.inTransaction(() -> {
			organizationDAO.registerOrganization(org);

			patientDAO.persistPatient(pat1);
			patientDAO.persistPatient(pat2);
			patientDAO.persistPatient(pat3);

			providerDAO.persistProvider(provider);

			rosterDAO.persistEntity(goodRoster);
			rosterDAO.persistEntity(badRoster);

			relationshipDAO.addAttributionRelationship(attribution1);
			relationshipDAO.addAttributionRelationship(attribution2);
			relationshipDAO.addAttributionRelationship(attribution3);
		});

		List<AttributionRelationship> attributions = relationshipDAO.lookupAttributionRelationships(
			goodRoster.getId(), List.of(pat1.getID(), pat2.getID(), pat3.getID()));

		assertEquals(2, attributions.size());
		assertTrue(attributions.contains(attribution1));
		assertTrue(attributions.contains(attribution2));
		assertFalse(attributions.contains(attribution3));
	}

	// If the Hibernate sequence generator increment size and the DB sequence increment don't match we'll start getting
	// failed inserts for duplicate keys.  Check that here to prevent someone from accidentally changing one and not the
	// other.
	@Test
        @DisplayName("Verify internal database key integrity 🥳")
	public void test_SequenceIncrementSizeMatches() throws ClassNotFoundException, NoSuchFieldException {
		Field attributionID = ClassLoader.getSystemClassLoader()
			.loadClass("gov.cms.dpc.common.entities.AttributionRelationship")
			.getDeclaredField("attributionID");

                SequenceGenerator annotation = attributionID.getAnnotation(SequenceGenerator.class);

                 // Directly access the allocationSize property
                int hibernateIncrement = annotation.allocationSize();

		Session session = db.getSessionFactory().getCurrentSession();
		String sql = "select increment_by from pg_sequences where sequencename = 'attributions_id_seq'";
		int dbIncrement = (session.createNativeQuery(sql, BigInteger.class).getSingleResult()).intValue();

		assertEquals(hibernateIncrement, dbIncrement);
	}
}
