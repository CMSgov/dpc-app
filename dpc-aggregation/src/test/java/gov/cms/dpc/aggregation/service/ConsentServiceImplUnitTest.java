package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.aggregation.jdbi.ConsentDAO;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;

class ConsentServiceImplUnitTest {

    final String testFhirUrl = "http://test-fhir-url";

    private ConsentService consentService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    public ConsentDAO mockConsentDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consentService = new ConsentServiceImpl(mockConsentDao, testFhirUrl);
    }

    @Test
    void getSingleConsent() {
        final String testMbi = "0OO0OO0OO00";

        List<ConsentEntity> entities = List.of(createConsentEntity(testMbi), createConsentEntity(testMbi));
        Mockito.when(mockConsentDao.findByMbis(anyList())).thenReturn(entities);

        Optional<List<ConsentResult>> results =  consentService.getConsent(testMbi);
        assertTrue(results.isPresent(), "Expected optional to have a value");
        assertEquals(2, results.get().size(), "Expected 2 consent results");
    }

    @Test
    void getMultipleConsent() {
        String mbi1 = "0OO0OO0OO00";
        String mbi2 = "0OO0OO0OO01";

        List<ConsentEntity> entities = List.of(createConsentEntity(mbi1), createConsentEntity(mbi1), createConsentEntity(mbi2));
        Mockito.when(mockConsentDao.findByMbis(anyList())).thenReturn(entities);

        Optional<List<ConsentResult>> optionalResults = consentService.getConsent(List.of(mbi1, mbi2));
        assertTrue(optionalResults.isPresent());
        List<ConsentResult> results = optionalResults.get();
        assertEquals(3, results.size());
    }

    @Test
    void testNoConsent() {
        final String testMbi = "0OO0OO0OO00";

        List<ConsentEntity> entities = List.of();
        Mockito.when(mockConsentDao.findByMbis(anyList())).thenReturn(entities);

        Optional<List<ConsentResult>> results =  consentService.getConsent(testMbi);
        assertTrue(results.isPresent(), "Expected optional to have a value.");
        assertEquals(0, results.get().size(), "Expected consent results to be an empty list");
    }

    private ConsentEntity createConsentEntity(String mbi) {
        return ConsentEntity.defaultConsentEntity(Optional.of(UUID.randomUUID()), Optional.of("test_hicn"), Optional.of(mbi));
    }
}
