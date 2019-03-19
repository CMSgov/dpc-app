package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultBlueButtonClient implements BlueButtonClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultBlueButtonClient.class);

    private IGenericClient client;


    public DefaultBlueButtonClient(IGenericClient client){
        this.client = client;
    }

    public Patient requestPatientFromServer(String patientID) throws ResourceNotFoundException {
        logger.debug("Attempting to fetch patient ID {} from baseURL: {}", patientID, client.getServerBase());
        return client.read().resource(Patient.class).withId(patientID).execute();

    }

    public Bundle requestEOBBundleFromServer(String patientID) throws ResourceNotFoundException {
        // TODO: need to implement some kind of pagination? EOB bundles can be HUGE
        logger.debug("Attempting to fetch patient ID {} from baseURL: {}", patientID, client.getServerBase());
        return client.search()
                .forResource(ExplanationOfBenefit.class)
                .where(ExplanationOfBenefit.PATIENT.hasId(patientID))
                .returnBundle(Bundle.class)
                .execute();

    }
}
