package gov.cms.dpc.aggregation.client.consent;

import io.reactivex.Maybe;
import org.hl7.fhir.dstu3.model.Consent;

public interface ConsentClient {
    Maybe<Consent> fetchConsentByMBI(String patientID);
}
