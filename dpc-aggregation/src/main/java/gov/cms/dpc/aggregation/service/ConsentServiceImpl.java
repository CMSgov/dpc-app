package gov.cms.dpc.aggregation.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Consent;

import javax.inject.Named;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConsentServiceImpl implements ConsentService {

    private final IGenericClient consentClient;

    public ConsentServiceImpl(@Named("consentClient") IGenericClient consentClient){
        this.consentClient = consentClient;
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
        List<String> fullMbis = mbis.stream()
                .map( mbi -> String.format("%s|%s", DPCIdentifierSystem.MBI.getSystem(), mbi) )
                .collect(Collectors.toList());

        return consentClient
                .search()
                .forResource(Consent.class)
                .encodedJson()
                .returnBundle(Bundle.class)
                .where(Consent.PATIENT.hasAnyOfIds(fullMbis))
                .execute();
    }
}
