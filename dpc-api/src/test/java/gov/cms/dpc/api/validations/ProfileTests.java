package gov.cms.dpc.api.validations;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.*;
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

        final Patient invalidPatient = new Patient();
        invalidPatient.addName().addGiven("test").setFamily("patient");

        final ICreateTyped patientCreate = client
                .create()
                .resource(invalidPatient)
                .encodedJson();

        final UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, patientCreate::execute, "Should fail with unfulfilled profile");

        // Try for a valid patient
        final Patient validPatient = invalidPatient.copy();
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
        bundle.addEntry().setResource(invalidPatient);
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

    @Test
    void testProviderProfile() {
        final IGenericClient client = APITestHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN);

        final Practitioner invalidPractitioner = new Practitioner();
        invalidPractitioner.addName().addGiven("Test").setFamily("Practitioner");

        final ICreateTyped practitionerCreate = client
                .create()
                .resource(invalidPractitioner)
                .encodedJson();

        assertThrows(UnprocessableEntityException.class, practitionerCreate::execute, "Should fail profile validation");

        final Practitioner validPractitioner = invalidPractitioner.copy();
        validPractitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-npi");

        final MethodOutcome created = client
                .create()
                .resource(validPractitioner)
                .encodedJson()
                .execute();

        final Bundle bundle = new Bundle();
        bundle.addEntry().setResource(invalidPractitioner);
        bundle.addEntry().setResource(validPractitioner);

        final Parameters params = new Parameters();
        params.addParameter().setResource(bundle);

        final IOperationUntypedWithInput<Bundle> practitionerSubmission = client
                .operation()
                .onType(Practitioner.class)
                .named("submit")
                .withParameters(params)
                .returnResourceType(Bundle.class)
                .encodedJson();

        assertThrows(UnprocessableEntityException.class, practitionerSubmission::execute, "Should throw a submission exception");
    }

}
