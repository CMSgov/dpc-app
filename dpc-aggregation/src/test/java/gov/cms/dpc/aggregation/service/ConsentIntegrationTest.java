package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.common.consent.entities.OptOutFileEntity;
import gov.cms.dpc.common.hibernate.consent.DPCConsentManagedSessionFactory;
import gov.cms.dpc.common.jdbi.ConsentDAO;
import gov.cms.dpc.testing.AbstractMultipleDAOTest;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

//@IntegrationTest
@ExtendWith(BufferedLoggerHandler.class)
public class ConsentIntegrationTest extends AbstractMultipleDAOTest {
    private ConsentDAO consentDAO;
    private ConsentService consentService;

    public ConsentIntegrationTest() {
        super(ConsentEntity.class, OptOutFileEntity.class);
    }

    @BeforeEach
    void setup() {
        // final Configuration conf = new Configuration();
        consentDAO = new ConsentDAO(new DPCConsentManagedSessionFactory(db.getSessionFactory()));
        consentService = new ConsentServiceImpl(
                consentDAO,
            "http://localhost:3200/api/v1"
        );
    }


    @Test
    void testConsentRecordsProcessed() {
        String mockMbi = "1SJ0A00AA00N";
        createMockConsentEntity(mockMbi);
        Optional<List<ConsentResult>> consentResults = consentService.getConsent(List.of(mockMbi));
        assertTrue(consentResults.isPresent());
        List<ConsentResult> optOutResults = consentResults.get();
        assertFalse(optOutResults.isEmpty());
    }

    private void createMockConsentEntity(String mockMbi) {
        ConsentEntity consentEntity = new ConsentEntity();
        consentEntity.setMbi(mockMbi);
        consentEntity.setPolicyCode("OPTOUT");
        consentEntity.setLoincCode("64292-6");
        consentEntity.setEffectiveDate(LocalDate.now());
        // consentEntity.setOptOutFile(fileEntity);
        db.inTransaction(() -> {
            consentDAO.persistConsent(consentEntity);
        });
    }

}
