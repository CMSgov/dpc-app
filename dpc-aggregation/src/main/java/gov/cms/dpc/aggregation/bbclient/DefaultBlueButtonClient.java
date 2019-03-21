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

    /**
     * Queries Blue Button server for patient data
     *
     * @param patientID The requested patient's ID
     * @return {@link Patient} A FHIR Patient resource
     * @throws ResourceNotFoundException when no such patient with the provided ID exists
     */
    public Patient requestPatientFromServer(String patientID) throws ResourceNotFoundException {
        logger.debug("Attempting to fetch patient ID {} from baseURL: {}", patientID, client.getServerBase());
        return client.read().resource(Patient.class).withId(patientID).execute();

    }

    /**
     * Queries Blue Button server for Explanations of Benefit associated with a given patient
     *
     * There are two edge cases to consider when pulling EoB data given a patientID:
     *  1. No patient with the given ID exists: if this is the case, BlueButton should return a Bundle with no
     *  entry, i.e. ret.hasEntry() will evaluate to false. For this case, the method will throw a
     *  {@link ResourceNotFoundException}
     *
     *  2. A patient with the given ID exists, but has no associated EoB records: if this is the case, BlueButton should
     *  return a Bundle with an entry of size 0, i.e. ret.getEntry().size() == 0. For this case, the method simply
     *  returns the Bundle it received from BlueButton to the caller, and the caller is responsible for handling Bundles
     *  that contain no EoBs.
     *
     * @param patientID The requested patient's ID
     * @return {@link Bundle} Containing a number (possibly 0) of {@link ExplanationOfBenefit} objects
     * @throws ResourceNotFoundException when the requested patient does not exist
     */
    public Bundle requestEOBBundleFromServer(String patientID) throws ResourceNotFoundException {
        // TODO: need to implement some kind of pagination? EOB bundles can be HUGE
        logger.debug("Attempting to fetch EOBs for patient ID {} from baseURL: {}", patientID, client.getServerBase());
        Bundle ret = client.search()
                .forResource(ExplanationOfBenefit.class)
                .where(ExplanationOfBenefit.PATIENT.hasId(patientID))
                .returnBundle(Bundle.class)
                .execute();

        if(ret.hasEntry()) {
            if(ret.getEntry().size() == 0) { // Case where patient exists, but
                return ret;
            }

            return ret;
        } else { // Case where patientID does not exist at all
            throw new ResourceNotFoundException("No patient found with ID: " + patientID);
        }

    }
}
