package gov.cms.dpc.aggregation.service;

import gov.cms.dpc.common.jdbi.ConsentDAO;
import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.converters.entities.ConsentEntityConverter;
import jakarta.inject.Named;
import org.hl7.fhir.dstu3.model.Consent;

import java.util.List;
import java.util.Optional;

public class ConsentServiceImpl implements ConsentService {
    private final ConsentDAO consentDAO;
    private final String fhirReferenceURL;

    public ConsentServiceImpl(@Named("consentDAO") ConsentDAO consentDAO, @Named("fhirReferenceURL") String fhirReferenceURL) {
        this.consentDAO = consentDAO;
        this.fhirReferenceURL = fhirReferenceURL;
    }

    @Override
    public Optional<List<ConsentResult>> getConsent(String mbi) {
        return getConsent(List.of(mbi));
    }

    @Override
    public Optional<List<ConsentResult>> getConsent(List<String> mbis) {
        final List<ConsentEntity> entities = doConsentSearch(mbis);

        return Optional.of(
            entities.stream().map( entity -> {
                Consent consent = ConsentEntityConverter.toFhir(entity, fhirReferenceURL);
                ConsentResult consentResult = new ConsentResult();
                consentResult.setActive(Consent.ConsentState.ACTIVE.equals(consent.getStatus()));
                consentResult.setConsentDate(consent.getDateTime());
                consentResult.setConsentId(consent.getId());
                consentResult.setPolicyType(ConsentResult.PolicyType.fromPolicyUrl(consent.getPolicyRule()));
                return consentResult;
            }).toList()
        );
    }

    private List<ConsentEntity> doConsentSearch(List<String> mbis) {
        List<String> fullMbis = mbis.stream()
                .map( mbi -> String.format("%s|%s", DPCIdentifierSystem.MBI.getSystem(), mbi) )
                .toList();

        return this.consentDAO.findByMbis(fullMbis);
    }
}
