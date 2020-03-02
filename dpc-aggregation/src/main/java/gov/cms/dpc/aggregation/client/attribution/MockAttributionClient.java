package gov.cms.dpc.aggregation.client.attribution;

import gov.cms.dpc.bluebutton.client.MockBlueButtonClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import io.reactivex.Single;
import org.hl7.fhir.dstu3.model.Patient;

public class MockAttributionClient implements AttributionClient {

    public MockAttributionClient() {
        // Not used
    }

    @Override
    public Single<Patient> fetchPatientByMBI(String mbi) {
        if (MockBlueButtonClient.TEST_PATIENT_MBIS.contains(mbi)) {
            final Patient patient = new Patient();
            patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue(mbi);
            return Single.just(patient);
        } else {
            return Single.error(() -> new IllegalArgumentException(String.format(AttributionClientImpl.EXCEPTION_FMT, mbi)));
        }
    }
}
