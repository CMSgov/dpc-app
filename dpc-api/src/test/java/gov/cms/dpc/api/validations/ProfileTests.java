package gov.cms.dpc.api.validations;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IOperationUntypedWithInput;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import gov.cms.dpc.api.AbstractSecureApplicationTest;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.APIAuthHelpers;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProfileTests extends AbstractSecureApplicationTest {

    @Inject
    private ProfileTests() {
        // Not used
    }

    @Test
    void testPatientProfile() {
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);
        // Create a new patient record

        final Patient invalidPatient = new Patient();
        invalidPatient.addName().addGiven("test").setFamily("patient");

        final ICreateTyped patientCreate = client
                .create()
                .resource(invalidPatient)
                .encodedJson();

        assertThrows(UnprocessableEntityException.class, patientCreate::execute, "Should fail with unfulfilled profile");

        final Parameters vParams = new Parameters();
        vParams.addParameter().setResource(invalidPatient);
        // Check that validate works
        final OperationOutcome outcome = client
                .operation()
                .onType(Patient.class)
                .named("validate")
                .withParameters(vParams)
                .returnResourceType(OperationOutcome.class)
                .encodedJson()
                .execute();

        assertEquals(3, outcome.getIssue().size(), "Should have validation failures");

        // Try for a valid patient
        final Patient validPatient = invalidPatient.copy();
        validPatient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("0O00O00OO00");
        validPatient.setGender(Enumerations.AdministrativeGender.MALE);
        validPatient.setBirthDate(Date.valueOf("1990-01-01"));
        client
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
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);

        final Practitioner invalidPractitioner = new Practitioner();
        invalidPractitioner.addName().addGiven("Test").setFamily("Practitioner");

        final ICreateTyped practitionerCreate = client
                .create()
                .resource(invalidPractitioner)
                .encodedJson();

        assertThrows(UnprocessableEntityException.class, practitionerCreate::execute, "Should fail profile validation");

        final Parameters vParams = new Parameters();
        vParams.addParameter().setResource(invalidPractitioner);
        // Check that validate works
        final OperationOutcome outcome = client
                .operation()
                .onType(Practitioner.class)
                .named("validate")
                .withParameters(vParams)
                .returnResourceType(OperationOutcome.class)
                .encodedJson()
                .execute();

        assertEquals(1, outcome.getIssue().size(), "Should have validation failures");

        final Practitioner validPractitioner = invalidPractitioner.copy();
        validPractitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("1232312110");

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

    @Test
    @Disabled
        // Disabled until DPC-614 and DPC-616 are merged.
    void testAttributionProfile() {
        final IGenericClient client = APIAuthHelpers.buildAuthenticatedClient(ctx, getBaseURL(), ORGANIZATION_TOKEN, PUBLIC_KEY_ID, PRIVATE_KEY);

        final Group invalidGroup = new Group();
        invalidGroup.addMember().setEntity(new Reference("Patient/strange-patient"));

        final ICreateTyped groupCreation = client
                .create()
                .resource(invalidGroup)
                .encodedJson();

        final UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, groupCreation::execute, "Should throw a creation exception");

        final Group validGroup = invalidGroup.copy();
        validGroup.setType(Group.GroupType.PERSON);
        validGroup.setActive(true);
        validGroup.setActual(true);
        final CodeableConcept concept = new CodeableConcept();
        concept.addCoding().setCode("attributed-to");
        validGroup.addCharacteristic().setCode(concept).setExclude(false).setValue(new BooleanType(false));

        final ICreateTyped groupCreation2 = client
                .create()
                .resource(validGroup)
                .encodedJson();

        // Since we're creating a group with a patient that doesn't exist, we should throw an error, just not a validation one.
        assertThrows(InternalErrorException.class, groupCreation2::execute, "Should thrown internal error");
    }
}
