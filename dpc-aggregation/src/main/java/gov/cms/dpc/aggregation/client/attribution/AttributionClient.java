package gov.cms.dpc.aggregation.client.attribution;

import io.reactivex.Single;
import org.hl7.fhir.dstu3.model.Patient;

public interface AttributionClient {

    Single<Patient> fetchPatientByMBI(String mbi);
}
