package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultBlueButtonClient implements BlueButtonClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultBlueButtonClient.class);

    private IGenericClient client;


    public DefaultBlueButtonClient(IGenericClient client){
        this.client = client;
    }

    public Patient requestFHIRFromServer(String beneficiaryID) {

        try {
            logger.debug("Attempting to fetch patient ID {} from baseURL: {}", beneficiaryID, client.getServerBase());
            return client.read().resource(Patient.class).withUrl(buildSearchUrl(beneficiaryID)).execute();

        } catch (ResourceNotFoundException ex) {
            throw new BlueButtonClientException("Could not find beneficiary with ID: " + beneficiaryID, ex);
        }
    }

    private String buildSearchUrl(String beneficiaryID) {
        return client.getServerBase() + "Patient/" + beneficiaryID;
    }
}
