package gov.cms.dpc.aggregation.engine.suppression;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.dpc.aggregation.client.attribution.AttributionClient;
import gov.cms.dpc.aggregation.client.attribution.AttributionClientImpl;
import gov.cms.dpc.aggregation.client.consent.ConsentClient;
import gov.cms.dpc.aggregation.exceptions.SuppressionException;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.entities.ConsentEntityConverter;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(BufferedLoggerHandler.class)
public class SuppressionEngineTest {

    private static ConsentClient consentClient;
    private static AttributionClient attributionClient;
    private static SuppressionEngine engine;

    @BeforeAll
    static void setup() {
        consentClient = Mockito.mock(ConsentClient.class);
        attributionClient = Mockito.mock(AttributionClient.class);
        engine = new SuppressionEngineImpl(attributionClient, consentClient, FhirContext.forDstu3());
    }

    @AfterEach
    void reset() {
        Mockito.reset(consentClient);
        Mockito.reset(attributionClient);
    }

    @Test
    void testNoAttribution() {
        final String testMBI = "12345F";
        final Throwable throwable = new IllegalArgumentException(String.format(AttributionClientImpl.EXCEPTION_FMT, testMBI));

        Mockito.when(attributionClient.fetchPatientByMBI(testMBI))
                .thenThrow(throwable);

        engine.processSuppression(testMBI)
                .test()
                .assertError(throwable);

        // Verify mocks
        Mockito.verifyNoInteractions(consentClient);
        Mockito.verify(attributionClient, Mockito.times(1)).fetchPatientByMBI(Mockito.eq(testMBI));
    }

    @Test
    void testOptIn() {
        final UUID patientID = UUID.randomUUID();
        final String testMBI = "12345F";

        Mockito.when(attributionClient.fetchPatientByMBI(testMBI))
                .thenAnswer(answer -> {
                    final Patient patient = new Patient();
                    patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(testMBI);
                    patient.setId(new IdType("Patient", patientID.toString()));
                    return patient;
                });

        Mockito.when(consentClient.fetchConsentByMBI(patientID.toString()))
                .thenAnswer(answer -> {
                    final ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.empty(), Optional.of(patientID.toString()));
                    ce.setPolicyCode(ConsentEntity.OPT_IN);
                    return Optional.of(ConsentEntityConverter.convert(ce, "http://fake.org", "http://fhir.starter"));
                });

        engine.processSuppression(testMBI)
                .test()
                .assertComplete();

        // Verify mocks
        Mockito.verify(consentClient, Mockito.times(1)).fetchConsentByMBI(Mockito.eq(patientID.toString()));
        Mockito.verify(attributionClient, Mockito.times(1)).fetchPatientByMBI(Mockito.eq(testMBI));
    }

    @Test
    void testOptOut() {
        final UUID patientID = UUID.randomUUID();
        final String testMBI = "12345F";

        final SuppressionException exception = new SuppressionException(SuppressionException.SuppressionReason.OPT_OUT, testMBI, "Patient has opted-out");

        Mockito.when(attributionClient.fetchPatientByMBI(testMBI))
                .thenAnswer(answer -> {
                    final Patient patient = new Patient();
                    patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(testMBI);
                    patient.setId(new IdType("Patient", patientID.toString()));
                    return patient;
                });

        Mockito.when(consentClient.fetchConsentByMBI(patientID.toString()))
                .thenAnswer(answer -> {
                    final ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.empty(), Optional.of(patientID.toString()));
                    ce.setPolicyCode(ConsentEntity.OPT_OUT);
                    return Optional.of(ConsentEntityConverter.convert(ce, "http://fake.org", "http://fhir.starter"));
                });

        engine.processSuppression(testMBI)
                .test()
                .assertError(exception);

        // Verify mocks
        Mockito.verify(consentClient, Mockito.times(1)).fetchConsentByMBI(Mockito.eq(patientID.toString()));
        Mockito.verify(attributionClient, Mockito.times(1)).fetchPatientByMBI(Mockito.eq(testMBI));
    }

    @Test
    void testNoConsent() {
        final UUID patientID = UUID.randomUUID();
        final String testMBI = "12345F";

        Mockito.when(attributionClient.fetchPatientByMBI(testMBI))
                .thenAnswer(answer -> {
                    final Patient patient = new Patient();
                    patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(testMBI);
                    patient.setId(new IdType("Patient", patientID.toString()));
                    return patient;
                });

        Mockito.when(consentClient.fetchConsentByMBI(patientID.toString()))
                .thenAnswer(answer -> Optional.empty());

        engine.processSuppression(testMBI)
                .test()
                .assertComplete();

        // Verify mocks
        Mockito.verify(consentClient, Mockito.times(1)).fetchConsentByMBI(Mockito.eq(patientID.toString()));
        Mockito.verify(attributionClient, Mockito.times(1)).fetchPatientByMBI(Mockito.eq(testMBI));
    }
}
