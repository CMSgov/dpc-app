package gov.cms.dpc.aggregation.client.consent;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import io.reactivex.Maybe;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Consent;

import javax.inject.Inject;
import javax.inject.Named;

public class ConsentClientImpl implements ConsentClient {

    private final IGenericClient client;

    @Inject
    ConsentClientImpl(@Named("consent") IGenericClient client) {
        this.client = client;
    }

    @Override
    public Maybe<Consent> fetchConsentByMBI(String patientID) {
        return Maybe.fromCallable(() -> this.fetchConsent(patientID))
                .map(bundle -> {
                    if (bundle.isEmpty()) {
                        return null;
                    } else {
                        return (Consent) bundle.getEntryFirstRep().getResource();
                    }
                });
    }

    private Bundle fetchConsent(String patientID) {
        return this.client
                .search()
                .forResource(Consent.class)
                .where(Consent.PATIENT.hasId(String.format("%s|%s", DPCIdentifierSystem.MBI.getSystem(), patientID)))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();
    }
}
