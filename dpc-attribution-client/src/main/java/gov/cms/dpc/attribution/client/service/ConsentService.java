package gov.cms.dpc.attribution.client.service;

import gov.cms.dpc.common.jdbi.ConsentDAO;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.converters.entities.r4.ConsentEntityConverter;
import jakarta.inject.Named;
import org.hl7.fhir.r4.model.Consent;
import org.hl7.fhir.r4.model.Patient;
import gov.cms.dpc.fhir.r4.FHIRExtractors;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConsentService {
    private final ConsentDAO consentDAO;
    private final String fhirReferenceURL;

    public ConsentService(@Named("consentDAO") ConsentDAO consentDAO, @Named("fhirReferenceURL") String fhirReferenceURL) {
        this.consentDAO = consentDAO;
        this.fhirReferenceURL = fhirReferenceURL;
    }

    /**
     * Filters out patients whose MBIs have opted out.
     */
    public List<Patient> filterConsent(List<Patient> patients) {
        final List<String> mbis = patients.stream()
            .map(FHIRExtractors::getPatientMBI)
            .distinct()
            .toList();
        if (mbis.isEmpty()) {
            return List.of();
        }

        // Grab the consent records for all MBIs in one DB lookup and build a map of
        // MBI -> ConsentResult(s)
        final Map<String, List<ConsentResult>> consentsByMbi = doConsentSearch(mbis).stream()
            .collect(Collectors.groupingBy(
                ConsentEntity::getMbi,
                Collectors.mapping(this::toConsentResult, Collectors.toList())));

        // Filter for patients that haven't opted out and return list
        return patients.stream()
            .filter(patient -> !isOptedOut(Optional.of(
                consentsByMbi.getOrDefault(FHIRExtractors.getPatientMBI(patient), List.of()))))
            .toList();
    }

    private ConsentResult toConsentResult(ConsentEntity entity) {
        final Consent consent = ConsentEntityConverter.toFHIR(entity, fhirReferenceURL);
        final ConsentResult consentResult = new ConsentResult();
        consentResult.setActive(Consent.ConsentState.ACTIVE.equals(consent.getStatus()));
        consentResult.setConsentDate(consent.getDateTime().toInstant());
        consentResult.setConsentId(consent.getId());
        consentResult.setPolicyType(ConsentResult.PolicyType.fromPolicyCode(consent.getPolicyRule().getCodingFirstRep().getCode()));
        return consentResult;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private boolean isOptedOut(Optional<List<ConsentResult>> consentResultsOptional) {
        if (consentResultsOptional.isPresent()) {
            // Filter out future opt outs and find the latest current one.
            final Instant now = Instant.now();
            final List<ConsentResult> consentResults = consentResultsOptional.get().stream()
                .filter(consent -> !consent.getConsentDate().isAfter(now))
                .toList();
            if (consentResults.isEmpty()) {
                return false;
            }
            final ConsentResult latestConsent = Collections.max(consentResults, Comparator.comparing(ConsentResult::getConsentDate));
            final boolean isActive = latestConsent.isActive();
            final boolean isOptOut = ConsentResult.PolicyType.OPT_OUT.equals(latestConsent.getPolicyType());
            return isActive && isOptOut;
        }
        return true;
    }

    private List<ConsentEntity> doConsentSearch(List<String> mbis) {
        return this.consentDAO.findByMbis(mbis);
    }
}
