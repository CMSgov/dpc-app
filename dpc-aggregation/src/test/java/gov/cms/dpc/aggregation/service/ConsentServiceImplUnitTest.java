package gov.cms.dpc.aggregation.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.IQuery;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class ConsentServiceImplUnitTest {

    private ConsentService consentService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    public IGenericClient mockConsentClient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        consentService = new ConsentServiceImpl(mockConsentClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getSingleConsent() {
        final String testMbi = "0OO0OO0OO00";

        Bundle bundle = new Bundle();
        IQuery<IBaseBundle> queryExec = Mockito.mock(IQuery.class, Answers.RETURNS_DEEP_STUBS);
        Mockito.when(mockConsentClient.search().forResource(Consent.class).encodedJson()).thenReturn(queryExec);
        IQuery<Bundle> mockQuery = Mockito.mock(IQuery.class);
        Mockito.when(queryExec.returnBundle(any(Class.class)).where(any(ICriterion.class))).thenReturn(mockQuery);
        Mockito.when(mockQuery.execute()).thenReturn(bundle);

        bundle.addEntry(new Bundle.BundleEntryComponent().setResource(createTestConsent(testMbi)));
        bundle.addEntry(new Bundle.BundleEntryComponent().setResource(createTestConsent(testMbi)));

        Optional<List<ConsentResult>> results =  consentService.getConsent(testMbi);
        assertTrue(results.isPresent(), "Expected optional to have a value");
        assertEquals(2, results.get().size(), "Expected 2 consent results");
    }

    @Test
    public void getMultipleConsent() {
        String mbi1 = "0OO0OO0OO00";
        String mbi2 = "0OO0OO0OO01";

        Bundle returnBundle = new Bundle();
        returnBundle.addEntry(new Bundle.BundleEntryComponent().setResource(createTestConsent(mbi1)));
        returnBundle.addEntry(new Bundle.BundleEntryComponent().setResource(createTestConsent(mbi1)));
        returnBundle.addEntry(new Bundle.BundleEntryComponent().setResource(createTestConsent(mbi2)));

        IQuery<IBaseBundle> queryExec = Mockito.mock(IQuery.class, Answers.RETURNS_DEEP_STUBS);
        IQuery<Bundle> mockQuery = Mockito.mock(IQuery.class);

        Mockito.when(mockConsentClient.search().forResource(Consent.class).encodedJson()).thenReturn(queryExec);
        Mockito.when(queryExec.returnBundle(Bundle.class).where(any(ICriterion.class))).thenReturn(mockQuery);
        Mockito.when(mockQuery.execute()).thenReturn(returnBundle);

        Optional<List<ConsentResult>> optionalResults = consentService.getConsent(List.of(mbi1, mbi2));

        assertTrue(optionalResults.isPresent());

        List<ConsentResult> results = optionalResults.get();
        assertEquals(3, results.size());
    }

    @Test
    public void testNoConsent() {
        final String testMbi = "0OO0OO0OO00";

        Bundle bundle = new Bundle();
        IQuery<IBaseBundle> queryExec = Mockito.mock(IQuery.class, Answers.RETURNS_DEEP_STUBS);
        Mockito.when(mockConsentClient.search().forResource(Consent.class).encodedJson()).thenReturn(queryExec);
        IQuery<Bundle> mockQuery = Mockito.mock(IQuery.class);
        Mockito.when(queryExec.returnBundle(any(Class.class)).where(any(ICriterion.class))).thenReturn(mockQuery);
        Mockito.when(mockQuery.execute()).thenReturn(bundle);

        Optional<List<ConsentResult>> results =  consentService.getConsent(testMbi);
        assertTrue(results.isPresent(), "Expected optional to have a value.");
        assertEquals(0, results.get().size(), "Expected consent results to be an empty list");
    }

    private Consent createTestConsent(String mbi){
        Consent consent = new Consent();

        consent.setStatus(Consent.ConsentState.ACTIVE);

        Coding categoryCoding = new Coding("http://loinc.org","64292-6", null);
        CodeableConcept category = new CodeableConcept();
        category.setCoding(List.of(categoryCoding));
        consent.setCategory(List.of(category));

        String patientRefPath = "/Patient?identity=|"+mbi;
        consent.setPatient(new Reference("http://api.url" + patientRefPath));

        Date date = Date.from(Instant.now());
        consent.setDateTime(date);

        Reference orgRef = new Reference("Organization/" + UUID.randomUUID().toString());
        consent.setOrganization(List.of(orgRef));

        String policyUrl = "http://hl7.org/fhir/ConsentPolicy/opt-out";
        consent.setPolicyRule(policyUrl);

        consent.setId(UUID.randomUUID().toString());
        return consent;
    }
}