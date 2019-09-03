package gov.cms.dpc.api.validations;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.Test;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileTests extends AbstractSecureApplicationTest {

    ProfileTests() {
        // Not used
    }

    @Test
    void testPatientProfile() {
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN);
        // Create a new patient record

        final Patient patient = new Patient();
        patient.addName().addGiven("test").setFamily("patient");

        final ICreateTyped patientCreate = client
                .create()
                .resource(patient)
                .encodedJson();

        final UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, patientCreate::execute, "Should fail with unfulfilled profile");

        // Try for a valid patient
        final Patient validPatient = patient.copy();
        validPatient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");
        validPatient.setGender(Enumerations.AdministrativeGender.MALE);
        validPatient.setBirthDate(Date.valueOf("1990-01-01"));

        final MethodOutcome created = client
                .create()
                .resource(validPatient)
                .encodedJson()
                .execute();


        // Now, try a bulk submission, which should fail

        final Bundle bundle = new Bundle();
        bundle.addEntry().setResource(patient);
        bundle.addEntry().setResource(validPatient);

        final Parameters params = new Parameters();
        params.addParameter().setResource(bundle);

        final IOperationUntypedWithInput<Bundle> patientSubmission = client
                .operation()
                .onType(Patient.class)
                .named("submit")
                .withParameters(params)
                .returnResourceType(Bundle.class)
                .encodedJson();

        assertThrows(UnprocessableEntityException.class, patientSubmission::execute, "Should throw a submission exception");
    }

}
