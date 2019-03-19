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

    public Patient requestPatientFromServer(String patientID) {
        try {
            logger.debug("Attempting to fetch patient ID {} from baseURL: {}", patientID, client.getServerBase());
            return client.read().resource(Patient.class).withId(patientID).execute();

        } catch (ResourceNotFoundException ex) {
            throw new BlueButtonClientException("Could not find patient with ID: " + patientID, ex);
        }
    }

    public Bundle requestEOBBundleFromServer(String patientID) {
        try {
            // TODO: need to implement some kind of pagination? EOB bundles can be HUGE
            logger.debug("Attempting to fetch patient ID {} from baseURL: {}", patientID, client.getServerBase());
            return client.search()
                    .forResource(ExplanationOfBenefit.class)
                    .where(ExplanationOfBenefit.PATIENT.hasId(patientID))
                    .returnBundle(Bundle.class)
                    .execute();

        } catch (ResourceNotFoundException ex) {
            throw new BlueButtonClientException("Could not find EOBs for patient with ID: " + patientID, ex);
        }
    }
}
