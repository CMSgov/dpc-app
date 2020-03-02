package gov.cms.dpc.aggregation.client.consent;

import gov.cms.dpc.common.consent.entities.ConsentEntity;
import gov.cms.dpc.fhir.converters.FHIREntityConverter;
import gov.cms.dpc.fhir.converters.entities.ConsentEntityConverter;
import io.reactivex.Maybe;
import org.hl7.fhir.dstu3.model.Consent;

import java.util.Optional;

public class MockConsentClient implements ConsentClient {

    public static final String PATIENT_OPT_OUT = "1SQ3F00AA00";
    public static final String PATIENT_OPT_IN = "4SP0P00AA00";

    private final FHIREntityConverter converter;

    public MockConsentClient() {
        this.converter = FHIREntityConverter.initialize();
    }

    @Override
    public Maybe<Consent> fetchConsentByMBI(String patientID) {

        //
        if (!(patientID.equals(PATIENT_OPT_IN) || patientID.equals(PATIENT_OPT_OUT))) {
            return Maybe.empty();
        } else {
            final ConsentEntity ce = ConsentEntity.defaultConsentEntity(Optional.empty(), Optional.empty(), Optional.of(patientID));
            if (patientID.equals(PATIENT_OPT_OUT)) {
                ce.setPolicyCode(ConsentEntity.OPT_OUT);
            }

            return Maybe.just(ConsentEntityConverter.convert(ce, "http://fake.org", "http://fhir.starter"));
        }
    }
}
