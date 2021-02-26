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
        final Bundle bundle = doConsentSearch(mbi);

        List<ConsentResult> results = bundle.getEntry().stream().map(entryComponent -> {
            Consent consent = (Consent) entryComponent.getResource();
            ConsentResult consentResult = new ConsentResult();
            consentResult.setActive(Consent.ConsentState.ACTIVE.equals(consent.getStatus()));
            consentResult.setConsentDate(consent.getDateTime());
            consentResult.setConsentId(consent.getId());
            consentResult.setPolicyType(ConsentResult.PolicyType.fromPolicyUrl(consent.getPolicyRule()));
            return consentResult;
        }).collect(Collectors.toList());

        return Optional.of(results);
    }

    private Bundle doConsentSearch(String mbi){
        final String mbiIdentifier = String.format("%s|%s", DPCIdentifierSystem.MBI.getSystem(), mbi);
        return consentClient
                .search()
                .forResource(Consent.class)
                .encodedJson()
                .returnBundle(Bundle.class)
                .where(Consent.PATIENT.hasId(mbiIdentifier))
                .execute();
    }
}
