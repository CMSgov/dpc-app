package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PractitionerResourceTest extends AbstractAttributionTest {

    private PractitionerResourceTest() {
        // Not used
    }

    @Test
    void testPractitionerReadWrite() {

        final Practitioner practitioner = createPractitionerResource("test-npi-1");
        final IGenericClient client = createFHIRClient();
        final MethodOutcome mo = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        final Practitioner pract2 = (Practitioner) mo.getResource();

        // Try to directly access

        final Practitioner pract3 = client
                .read()
                .resource(Practitioner.class)
                .withId(pract2.getId())
                .encodedJson()
                .execute();

        assertTrue(pract2.equalsDeep(pract3), "Created and fetched resources should be identical");

        // Delete it and make sure it's gone.

        client
                .delete()
                .resource(pract3)
                .encodedJson()
                .execute();

        final IReadExecutable<Practitioner> deletedRead = client
                .read()
                .resource(Practitioner.class)
                .withId(pract3.getId())
                .encodedJson();

        assertThrows(ResourceNotFoundException.class, deletedRead::execute, "Should not be able to find the provider");


        // Try to delete it again
        final IDeleteTyped deleteOp = client
                .delete()
                .resource(pract3)
                .encodedJson();

        assertThrows(ResourceNotFoundException.class, deleteOp::execute, "Should not be able to find the provider");
    }

    @Test
    void testPractitionerSearch() {

        final Practitioner practitioner = createPractitionerResource("test-npi-1");
        final IGenericClient client = createFHIRClient();

        final MethodOutcome outcome = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        final Practitioner pract2 = (Practitioner) outcome.getResource();

        // Try to fetch all the patients
        final Bundle providers = client
                .search()
                .forResource(Practitioner.class)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        // We expect that the existing seeds already exist, so this means we have 8 + 1 providers
        assertEquals(9, providers.getEntry().size(), "Should have all 9 providers provider");

        // Try to search for the provider, we should get the same results
        final Bundle searchedProviders = client
                .search()
                .forResource(Practitioner.class)
                .where(Patient.IDENTIFIER.exactly().identifier(pract2.getIdentifierFirstRep().getValue()))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, searchedProviders.getEntry().size(), "Searched should be the same");
    }


    @Test
    void testPractitionerUpdate() {
        final Practitioner practitioner = createPractitionerResource("test-npi-2");
        final IGenericClient client = createFHIRClient();

        final MethodOutcome outcome = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        final Practitioner pract2 = (Practitioner) outcome.getResource();

        pract2.getNameFirstRep().setFamily("Updated");

        client
                .update()
                .resource(pract2)
                .encodedJson()
                .execute();

        // Read it back

        final Practitioner pract3 = client
                .read()
                .resource(Practitioner.class)
                .withId(pract2.getId())
                .encodedJson()
                .execute();

        assertTrue(pract2.equalsDeep(pract3), "Updated values should match");
        assertFalse(pract3.equalsDeep(practitioner), "Should not match original");
    }

    private IGenericClient createFHIRClient() {
        // Upload it
        final FhirContext ctx = FhirContext.forDstu3();

        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return ctx.newRestfulGenericClient(getServerURL());
    }

    private static Practitioner createPractitionerResource(String NPI) {
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue(NPI);
        practitioner.addName()
                .setFamily("Practitioner").addGiven("Test");

        return practitioner;
    }
}
