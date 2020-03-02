package gov.cms.dpc.aggregation.client.consent;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Consent;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;

public class ConsentClientImpl implements ConsentClient {

    private final IGenericClient client;

    @Inject
    ConsentClientImpl(@Named("consent") IGenericClient client) {
        this.client = client;
    }

    @Override
    public Optional<Consent> fetchConsentByMBI(String patientID) {
        final Bundle bundle = this.client
                .search()
                .forResource(Consent.class)
                .where(Consent.PATIENT.hasId(patientID))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (bundle.getTotal() == 0) {
            return Optional.empty();
        }

        return Optional.of((Consent) bundle.getEntryFirstRep().getResource());
    }
}
