package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.common.consent.entities.OptOutFileEntity;
import gov.cms.dpc.common.hibernate.consent.DPCConsentManagedSessionFactory;
import gov.cms.dpc.common.jdbi.ConsentDAO;
import gov.cms.dpc.testing.AbstractMultipleDAOTest;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(BufferedLoggerHandler.class)
public class ConsentIntegrationTest extends AbstractMultipleDAOTest {
    private ConsentDAO consentDAO;
    private ConsentService consentService;

    public ConsentIntegrationTest() {
        super(ConsentEntity.class, OptOutFileEntity.class);
    }

    @BeforeEach
    void setup() {
        consentDAO = new ConsentDAO(new DPCConsentManagedSessionFactory(db.getSessionFactory()));
        consentService = new ConsentServiceImpl(
                consentDAO,
                "http://localhost:3200/api/v1"
        );
    }

    @Test
    void testGetConsent_rawStoredMbi_matchesRawSearchMbi() {
        String mockMbi = "1SJ0A00AA00N";
        createMockConsentEntity(mockMbi);

        Optional<List<ConsentResult>> consentResults = consentService.getConsent(List.of(mockMbi));

        assertTrue(consentResults.isPresent());
        assertEquals(1, consentResults.get().size());
    }

    @Test
    void testGetConsent_multipleRawStoredMbis_allMatchSearch() {
        String mbiOne = "1SJ0A00AA00N";
        String mbiTwo = "2TK1B11BB11P";
        createMockConsentEntity(mbiOne);
        createMockConsentEntity(mbiTwo);

        Optional<List<ConsentResult>> consentResults =
                consentService.getConsent(List.of(mbiOne, mbiTwo));

        assertTrue(consentResults.isPresent());
        assertEquals(2, consentResults.get().size());
    }

    @Test
    void testGetConsent_mixedList_onlyMbiWithConsentRecordReturned() {
        String mbiWithConsent = "1SJ0A00AA00N";
        String mbiWithoutConsent = "9ZZ9Z00ZZ00Z";
        createMockConsentEntity(mbiWithConsent);

        Optional<List<ConsentResult>> consentResults =
                consentService.getConsent(List.of(mbiWithConsent, mbiWithoutConsent));

        assertTrue(consentResults.isPresent());
        assertEquals(1, consentResults.get().size());
    }

    @Test
    void testGetConsent_mbiWithNoConsentRecord_returnsNoResults() {
        String mbiWithoutConsent = "9ZZ9Z00ZZ00Z";

        Optional<List<ConsentResult>> consentResults =
                consentService.getConsent(List.of(mbiWithoutConsent));

        assertTrue(consentResults.isEmpty() || consentResults.get().isEmpty());
    }

    private void createMockConsentEntity(String mockMbi) {
        ConsentEntity consentEntity = new ConsentEntity();
        consentEntity.setMbi(mockMbi);
        consentEntity.setPolicyCode("OPTOUT");
        consentEntity.setLoincCode("64292-6");
        consentEntity.setEffectiveDate(LocalDate.now());

        db.inTransaction(() -> {
            consentDAO.persistConsent(consentEntity);
        });
    }
}
