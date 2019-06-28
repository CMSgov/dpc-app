package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class PractitionerResourceTest extends AbstractAttributionTest {

    private PractitionerResourceTest() {
        // Not used
    }

    @Test
    void testPractitionerReadWrite() {
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue("test-npi-1");
        practitioner.addName()
                .setFamily("Practitioner").addGiven("Test");

        // Upload it
        final FhirContext ctx = FhirContext.forDstu3();

        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        final IGenericClient client = ctx.newRestfulGenericClient(getServerURL());

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
}
