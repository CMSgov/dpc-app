package gov.cms.dpc.aggregation.client.attribution;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;

import javax.inject.Inject;
import javax.inject.Named;

public class AttributionClientImpl implements AttributionClient {

    public static final String EXCEPTION_FMT = "Cannot find patient with MBI: %s";
    private final IGenericClient client;

    @Inject
    AttributionClientImpl(@Named("attribution") IGenericClient client) {
        this.client = client;
    }


    @Override
    public Patient fetchPatientByMBI(String mbi) {
        final Bundle patientBundle = this.client
                .search()
                .forResource(Patient.class)
                .where(Patient.IDENTIFIER.exactly().systemAndCode(DPCIdentifierSystem.MBI.getSystem(), mbi))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (patientBundle.isEmpty()) {
            throw new IllegalArgumentException(String.format(EXCEPTION_FMT, mbi));
        }

        return (Patient) patientBundle.getEntryFirstRep().getResource();
    }
}
