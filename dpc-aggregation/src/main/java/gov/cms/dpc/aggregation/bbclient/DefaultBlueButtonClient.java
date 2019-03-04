package gov.cms.dpc.aggregation.bbclient;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

public class DefaultBlueButtonClient implements BlueButtonClient {

    private static final Logger logger = LoggerFactory.getLogger(DefaultBlueButtonClient.class);

    private URL serverBaseUrl;
    private IGenericClient client;


    public DefaultBlueButtonClient(IGenericClient client, URL baseURL){
        this.client = client;
        this.serverBaseUrl = baseURL;
    }

    public Patient requestFHIRFromServer(String beneficiaryID) {

        try {
            return client.read().resource(Patient.class).withUrl(buildSearchUrl(beneficiaryID)).execute();

        } catch (MalformedURLException ex) {
            throw new BlueButtonClientException(
                    "There was an error building the URL from the patientID: " + beneficiaryID,
                    ex
            );

        } catch (ResourceNotFoundException ex) {
            throw new BlueButtonClientException("Could not find beneficiary with ID: " + beneficiaryID, ex);
        }
    }

    private String buildSearchUrl(String beneficiaryID) throws MalformedURLException {
        return new URL(serverBaseUrl, "Patient/" + beneficiaryID).toString();
    }
}
