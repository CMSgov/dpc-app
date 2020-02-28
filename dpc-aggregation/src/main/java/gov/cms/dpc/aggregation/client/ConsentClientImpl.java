package gov.cms.dpc.aggregation.client;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Consent;

import javax.inject.Inject;
import java.util.Optional;

public class ConsentClientImpl implements ConsentClient {

    private final IGenericClient client;

    @Inject
    ConsentClientImpl(IGenericClient client) {
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
