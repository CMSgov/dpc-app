package gov.cms.dpc.aggregation.service;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Consent;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConsentServiceImpl implements ConsentService {

    public ConsentServiceImpl(){
    }

    @Override
    public Optional<List<ConsentResult>> getConsent(String mbi) {
        return getConsent(List.of(mbi));
    }

    @Override
    public Optional<List<ConsentResult>> getConsent(List<String> mbis) {
        final Bundle bundle = doConsentSearch(mbis);

        return Optional.of(
            bundle.getEntry().stream().map( entry -> {
                Consent consent = (Consent) entry.getResource();

                ConsentResult consentResult = new ConsentResult();
                consentResult.setActive(Consent.ConsentState.ACTIVE.equals(consent.getStatus()));
                consentResult.setConsentDate(consent.getDateTime());
                consentResult.setConsentId(consent.getId());
                consentResult.setPolicyType(ConsentResult.PolicyType.fromPolicyUrl(consent.getPolicyRule()));
                return consentResult;
            }).collect(Collectors.toList())
        );
    }

    private Bundle doConsentSearch(List<String> mbis){
        return new Bundle();
    }
}
