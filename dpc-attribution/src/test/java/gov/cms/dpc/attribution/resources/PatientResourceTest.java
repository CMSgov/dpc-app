package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.Test;

import static gov.cms.dpc.attribution.AttributionTestHelpers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientResourceTest extends AbstractAttributionTest {

    private PatientResourceTest() {
        // Not used
    }

    @Test
    void testPatientReadWrite() {
        final Patient patient = createPatientResource("1871", DEFAULT_ORG_ID);

        patient.setManagingOrganization(new Reference(new IdType("Organization", DEFAULT_ORG_ID)));

        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final MethodOutcome outcome = client
                .create()
                .resource(patient)
                .encodedJson()
                .execute();

        assertTrue(outcome.getCreated(), "Should have been created");

        final Patient createdResource = (Patient) outcome.getResource();

        final Patient fetchedPatient = client
                .read()
                .resource(Patient.class)
                .withId(createdResource.getIdElement())
                .encodedJson()
                .execute();

        assertTrue(createdResource.equalsDeep(fetchedPatient), "Created and fetched should be equal");

        // Try to create them again

        final ICreateTyped secondCreation = client
                .create()
                .resource(patient)
                .encodedJson();

        assertThrows(InternalErrorException.class, secondCreation::execute, "Should not be able to create again");
    }

    @Test
    void testPatientSearch() {

    }

}
