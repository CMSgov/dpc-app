package gov.cms.dpc.aggregation.engine.suppression;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.aggregation.client.attribution.AttributionClient;
import gov.cms.dpc.aggregation.client.attribution.AttributionClientImpl;
import gov.cms.dpc.aggregation.client.attribution.MockAttributionClient;
import gov.cms.dpc.aggregation.client.consent.ConsentClient;
import gov.cms.dpc.aggregation.client.consent.MockConsentClient;
import gov.cms.dpc.aggregation.exceptions.SuppressionException;
import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.reactivex.Single;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(BufferedLoggerHandler.class)
public class SuppressionEngineTest {

    private static ConsentClient consentClient;
    private static AttributionClient attributionClient;
    private static SuppressionEngine engine;

    @BeforeAll
    static void setup() {
        consentClient = Mockito.spy(new MockConsentClient());
        attributionClient = Mockito.spy(new MockAttributionClient());
        engine = new SuppressionEngineImpl(attributionClient, consentClient, FhirContext.forDstu3());
    }

    @Test
    void testNoAttribution() {
        final String testMBI = "12345F";
        final IllegalArgumentException exception = new IllegalArgumentException(String.format(AttributionClientImpl.EXCEPTION_FMT, testMBI));

        engine.processSuppression(testMBI)
                .test()
                .assertError(error -> error.getMessage().equals(exception.getMessage()));

        // Verify mocks
        Mockito.verify(consentClient, Mockito.times(1)).fetchConsentByMBI(Mockito.eq(testMBI));
        Mockito.verify(attributionClient, Mockito.times(1)).fetchPatientByMBI(Mockito.eq(testMBI));
    }

    @Test
    void testOptIn() {
        engine.processSuppression(MockConsentClient.PATIENT_OPT_IN)
                .test()
                .assertComplete();

        // Verify mocks
        Mockito.verify(consentClient, Mockito.times(1)).fetchConsentByMBI(Mockito.eq(MockConsentClient.PATIENT_OPT_IN));
        Mockito.verify(attributionClient, Mockito.times(1)).fetchPatientByMBI(Mockito.eq(MockConsentClient.PATIENT_OPT_IN));
    }

    @Test
    void testOptOut() {

        final SuppressionException exception = new SuppressionException(SuppressionException.SuppressionReason.OPT_OUT, MockConsentClient.PATIENT_OPT_OUT, "Patient has opted-out");

        Mockito.when(attributionClient.fetchPatientByMBI(MockConsentClient.PATIENT_OPT_OUT))
                .thenAnswer(answer -> {
                    final Patient patient = new Patient();
                    patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(MockConsentClient.PATIENT_OPT_OUT);
                    return Single.just(patient);
                });

        engine.processSuppression(MockConsentClient.PATIENT_OPT_OUT)
                .test()
                .assertError(exception);

        // Verify mocks
        Mockito.verify(consentClient, Mockito.times(1)).fetchConsentByMBI(Mockito.eq(MockConsentClient.PATIENT_OPT_OUT));
        Mockito.verify(attributionClient, Mockito.times(1)).fetchPatientByMBI(Mockito.eq(MockConsentClient.PATIENT_OPT_OUT));
    }

    @Test
    void testNoConsent() {
        engine.processSuppression(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0))
                .test()
                .assertComplete();

        // Verify mocks
        Mockito.verify(consentClient, Mockito.times(1)).fetchConsentByMBI(Mockito.eq(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)));
        Mockito.verify(attributionClient, Mockito.times(1)).fetchPatientByMBI(Mockito.eq(MockBlueButtonClient.TEST_PATIENT_MBIS.get(0)));
    }
}
