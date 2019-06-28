package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PractitionerResourceTest extends AbstractAttributionTest {

    private PractitionerResourceTest() {
        // Not used
    }

    @Test
    void testPractitionerReadWrite() {

        final Practitioner practitioner = createPractitionerResource();

        final IGenericClient client = createFHIRClient();

        final MethodOutcome outcome = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        final Practitioner pract2 = (Practitioner) outcome.getResource();

        // Try to directly access

        final Practitioner pract3 = client
                .read()
                .resource(Practitioner.class)
                .withId(pract2.getId())
                .encodedJson()
                .execute();

        assertTrue(pract2.equalsDeep(pract3), "Created and fetched resources should be identical");
    }

    @Test
    void testPractitionerSearch() {

        final Practitioner practitioner = createPractitionerResource();
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

        // We expect that the existing seeds already exist, so this means we have 4 + 1 providers
        assertEquals(5, providers.getEntry().size(), "Should have one provider");

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

    private static Practitioner createPractitionerResource() {
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue("test-npi-1");
        practitioner.addName()
                .setFamily("Practitioner").addGiven("Test");

        return practitioner;
    }

    private IGenericClient createFHIRClient() {
        // Upload it
        final FhirContext ctx = FhirContext.forDstu3();

        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return ctx.newRestfulGenericClient(getServerURL());
    }
}
