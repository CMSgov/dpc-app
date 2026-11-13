package gov.cms.dpc.attribution.client.service;

import gov.cms.dpc.common.jdbi.ConsentDAO;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsentServiceUnitTest {

    final String testFhirUrl = "http://test-fhir-url";

    private ConsentService consentService;

    @Mock
    public ConsentDAO mockConsentDAO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consentService = new ConsentService(mockConsentDAO, testFhirUrl);
    }

    @Test
    void filterSinglePatientConsent() {
        final String testMbi = "0OO0OO0OO00";
        Patient patient = createPatient(testMbi);
        ConsentEntity optOut = createConsentEntity(testMbi);
        optOut.setPolicyCode(ConsentEntity.OPT_OUT);
        optOut.setEffectiveDate(LocalDate.of(2020, 1, 1));
        ConsentEntity optIn = createConsentEntity(testMbi);
        optIn.setEffectiveDate(LocalDate.of(2021, 1, 1));
        List<String> identifiers = List.of(testMbi);

        List<ConsentEntity> entities = List.of(optOut, optIn);
        Mockito.when(mockConsentDAO.findByMbis(identifiers)).thenReturn(entities);

        List<Patient> results = consentService.filterConsent(List.of(patient));
        assertEquals(List.of(patient), results);
        Mockito.verify(mockConsentDAO).findByMbis(identifiers);
        Mockito.verifyNoMoreInteractions(mockConsentDAO);
    }

    @Test
    void filterMultiplePatientConsent() {
        String mbi1 = "0OO0OO0OO00";
        String mbi2 = "0OO0OO0OO01";
        String mbi3 = "0OO0OO0OO02";
        Patient patient1 = createPatient(mbi1);
        Patient patient2 = createPatient(mbi2);
        Patient patient3 = createPatient(mbi3);
        ConsentEntity optIn = createConsentEntity(mbi1);
        optIn.setEffectiveDate(LocalDate.of(2020, 1, 1));
        ConsentEntity optOut = createConsentEntity(mbi1);
        optOut.setPolicyCode(ConsentEntity.OPT_OUT);
        optOut.setEffectiveDate(LocalDate.of(2021, 1, 1));
        List<String> identifiers = List.of(mbi1, mbi2, mbi3);

        List<ConsentEntity> entities = List.of(optIn, optOut, createConsentEntity(mbi2));
        Mockito.when(mockConsentDAO.findByMbis(identifiers)).thenReturn(entities);

        List<Patient> results = consentService.filterConsent(List.of(patient1, patient2, patient3, patient2));
        assertEquals(List.of(patient2, patient3, patient2), results);
        Mockito.verify(mockConsentDAO).findByMbis(identifiers);
        Mockito.verifyNoMoreInteractions(mockConsentDAO);
    }

    @Test
    void testNoConsent() {
        final String testMbi = "0OO0OO0OO00";
        Patient patient = createPatient(testMbi);
        List<String> identifiers = List.of(testMbi);

        Mockito.when(mockConsentDAO.findByMbis(identifiers)).thenReturn(List.of());

        List<Patient> results = consentService.filterConsent(List.of(patient));
        assertEquals(List.of(patient), results);
        Mockito.verify(mockConsentDAO).findByMbis(identifiers);
        Mockito.verifyNoMoreInteractions(mockConsentDAO);
    }

    @ParameterizedTest
    @ValueSource(strings = {ConsentEntity.OPT_IN, ConsentEntity.OPT_OUT})
    void testFutureConsentDoesNotOverrideCurrentOptOut(String futurePolicyCode) {
        final String testMbi = "0OO0OO0OO00";
        Patient patient = createPatient(testMbi);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        ConsentEntity optOut = createConsentEntity(testMbi);
        optOut.setPolicyCode(ConsentEntity.OPT_OUT);
        optOut.setEffectiveDate(today.minusDays(1));
        ConsentEntity futureConsent = createConsentEntity(testMbi);
        futureConsent.setPolicyCode(futurePolicyCode);
        futureConsent.setEffectiveDate(today.plusDays(1));
        List<String> identifiers = List.of(testMbi);
        Mockito.when(mockConsentDAO.findByMbis(identifiers)).thenReturn(List.of(optOut, futureConsent));

        List<Patient> results = consentService.filterConsent(List.of(patient));
        assertEquals(List.of(), results);
    }

    @ParameterizedTest
    @ValueSource(strings = {ConsentEntity.OPT_IN, ConsentEntity.OPT_OUT})
    void testOnlyFutureConsentDoesNotOverrideCurrentOptIn(String futurePolicyCode) {
        final String testMbi = "0OO0OO0OO00";
        Patient patient = createPatient(testMbi);
        ConsentEntity futureConsent = createConsentEntity(testMbi);
        futureConsent.setPolicyCode(futurePolicyCode);
        futureConsent.setEffectiveDate(LocalDate.now(ZoneOffset.UTC).plusDays(1));
        List<String> identifiers = List.of(testMbi);
        Mockito.when(mockConsentDAO.findByMbis(identifiers)).thenReturn(List.of(futureConsent));

        List<Patient> results = consentService.filterConsent(List.of(patient));
        assertEquals(List.of(patient), results);
    }

    private ConsentEntity createConsentEntity(String mbi) {
        return ConsentEntity.defaultConsentEntity(Optional.of(UUID.randomUUID()), Optional.of("test_hicn"),
                Optional.of(mbi));
    }

    private Patient createPatient(String mbi) {
        Patient patient = new Patient();
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(mbi);
        return patient;
    }
}
