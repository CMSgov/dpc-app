package gov.cms.dpc.aggregation.client.attribution;

import org.hl7.fhir.dstu3.model.Patient;

public interface AttributionClient {

    Patient fetchPatientByMBI(String mbi);
}
