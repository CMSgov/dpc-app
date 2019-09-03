package gov.cms.dpc.api.validations;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileTests extends AbstractSecureApplicationTest {

    ProfileTests() {
        // Not used
    }

    @Test
    void testInvalidProfile() {
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN);
        // Create a new patient record

        final Patient patient = new Patient();
        patient.setMeta(new Meta().addProfile(PatientProfile.PROFILE_URI));

        final ICreateTyped patientCreate = client
                .create()
                .resource(patient)
                .encodedJson();

        final UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, patientCreate::execute, "Should have thrown a bad exception");

        // Create a new practitioner record

        // Create a new Group record
    }

}
