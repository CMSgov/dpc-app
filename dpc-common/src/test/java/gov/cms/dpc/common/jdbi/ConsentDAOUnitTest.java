package gov.cms.dpc.common.jdbi;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.common.consent.entities.OptOutFileEntity;
import gov.cms.dpc.common.hibernate.consent.DPCConsentManagedSessionFactory;
import gov.cms.dpc.common.jdbi.ConsentDAO;
import gov.cms.dpc.testing.AbstractMultipleDAOTest;
import gov.cms.dpc.testing.utils.MBIUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsentDAOUnitTest extends AbstractMultipleDAOTest {
	public ConsentDAOUnitTest() {
		super(ConsentEntity.class, OptOutFileEntity.class);
	}

	private ConsentDAO consentDAO;

	@BeforeEach
    void setup() {
		consentDAO = new ConsentDAO(new DPCConsentManagedSessionFactory(db.getSessionFactory()));
	}

	@Test
	void test_ConsentDAO_writes_consent() {
		ConsentEntity consentEntity = createConsentEntity();

		db.inTransaction(() -> consentDAO.persistConsent(consentEntity));

		ConsentEntity returnedConsent = consentDAO.getConsent(consentEntity.getId()).get();
		assertEquals(consentEntity.getMbi(), returnedConsent.getMbi());
	}

	@Test
	void test_ConsentDAO_lists_consent() {
		db.inTransaction(() -> {
			consentDAO.persistConsent(createConsentEntity());
			consentDAO.persistConsent(createConsentEntity());
			consentDAO.persistConsent(createConsentEntity());
		});

		List<ConsentEntity> returnedList = consentDAO.list();
		assertEquals(3, returnedList.size());
	}

	@Test
	void test_ConsentDAO_findBy() {
		ConsentEntity consentEntity = createConsentEntity();
		consentEntity.setHicn("fake_hicn");

		db.inTransaction(() -> {
			consentDAO.persistConsent(consentEntity);
			consentDAO.persistConsent(createConsentEntity());
			consentDAO.persistConsent(createConsentEntity());
		});

		List<ConsentEntity> returnedList = consentDAO.findBy("hicn", "fake_hicn");
		assertEquals(1, returnedList.size());
		assertEquals(consentEntity.getId(), returnedList.get(0).getId());
	}

    @Test
    void test_ConsentDAO_findByMbis() {
        ConsentEntity entity1 = createConsentEntity();
        entity1.setMbi("fake_mbi_1");
        ConsentEntity entity2 = createConsentEntity();
        entity2.setMbi("fake_mbi_2");
        ConsentEntity entity3 = createConsentEntity();
        entity3.setMbi("fake_mbi_3");

        db.inTransaction(() -> {
            consentDAO.persistConsent(entity1);
            consentDAO.persistConsent(entity2);
            consentDAO.persistConsent(entity3);
        });

        List<String> mbis = List.of("fake_mbi_1", "fake_mbi_2");
        List<ConsentEntity> returnedList = consentDAO.findByMbis(mbis);
        assertEquals(2, returnedList.size());
        assertEquals(mbis, returnedList.stream().map(ConsentEntity::getMbi).toList());
    }

	private ConsentEntity createConsentEntity() {
		ConsentEntity consentEntity = new ConsentEntity();
		consentEntity.setMbi(MBIUtil.generateMBI());
		consentEntity.setEffectiveDate(LocalDate.now());
		return consentEntity;
	}
}
