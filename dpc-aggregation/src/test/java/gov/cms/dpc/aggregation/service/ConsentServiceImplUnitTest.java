package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.common.jdbi.ConsentDAO;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsentServiceImplUnitTest {

    final String testFhirUrl = "http://test-fhir-url";

    private ConsentService consentService;

    @Mock
    public ConsentDAO mockConsentDAO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consentService = new ConsentServiceImpl(mockConsentDAO, testFhirUrl);
    }

    @Test
    void getSingleConsent() {
        final String testMbi = "0OO0OO0OO00";

        ConsentEntity optOut = createConsentEntity(testMbi);
        optOut.setPolicyCode(ConsentEntity.OPT_OUT);
        List<ConsentEntity> entities = List.of(optOut, createConsentEntity(testMbi));
        Mockito.when(mockConsentDAO.findByMbis(List.of(testMbi))).thenReturn(entities);

        Optional<List<ConsentResult>> results =  consentService.getConsent(testMbi);
        assertTrue(results.isPresent(), "Expected optional to have a value");
        assertEquals(2, results.get().size(), "Expected 2 consent results");
        assertEquals(ConsentResult.PolicyType.OPT_OUT, results.get().getFirst().getPolicyType());
        Mockito.verify(mockConsentDAO).findByMbis(List.of(testMbi));
        Mockito.verifyNoMoreInteractions(mockConsentDAO);
    }

    @Test
    void getMultipleConsent() {
        String mbi1 = "0OO0OO0OO00";
        String mbi2 = "0OO0OO0OO01";

        List<ConsentEntity> entities = List.of(createConsentEntity(mbi1), createConsentEntity(mbi1), createConsentEntity(mbi2));
        Mockito.when(mockConsentDAO.findByMbis(List.of(mbi1, mbi2))).thenReturn(entities);

        Optional<List<ConsentResult>> optionalResults = consentService.getConsent(List.of(mbi1, mbi2));
        assertTrue(optionalResults.isPresent());
        List<ConsentResult> results = optionalResults.get();
        assertEquals(3, results.size());
        Mockito.verify(mockConsentDAO).findByMbis(List.of(mbi1, mbi2));
        Mockito.verifyNoMoreInteractions(mockConsentDAO);
    }

    @Test
    void testNoConsent() {
        final String testMbi = "0OO0OO0OO00";

        Mockito.when(mockConsentDAO.findByMbis(List.of(testMbi))).thenReturn(List.of());

        Optional<List<ConsentResult>> results =  consentService.getConsent(testMbi);
        assertTrue(results.isPresent(), "Expected optional to have a value.");
        assertEquals(0, results.get().size(), "Expected consent results to be an empty list");
        Mockito.verify(mockConsentDAO).findByMbis(List.of(testMbi));
        Mockito.verifyNoMoreInteractions(mockConsentDAO);
    }

    private ConsentEntity createConsentEntity(String mbi) {
        return ConsentEntity.defaultConsentEntity(Optional.of(UUID.randomUUID()), Optional.of("test_hicn"), Optional.of(mbi));
    }
}
