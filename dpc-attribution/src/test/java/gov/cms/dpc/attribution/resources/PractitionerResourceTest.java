package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import static gov.cms.dpc.attribution.AttributionTestHelpers.DEFAULT_ORG_ID;
import static gov.cms.dpc.attribution.AttributionTestHelpers.createFHIRClient;
import static org.junit.jupiter.api.Assertions.*;

class PractitionerResourceTest extends AbstractAttributionTest {

    private PractitionerResourceTest() {
        // Not used
    }

    @Test
    void testPractitionerReadWrite() {

        final Practitioner practitioner = AttributionTestHelpers.createPractitionerResource("test-npi-1");
        final IGenericClient client = createFHIRClient(ctx, getServerURL());
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

        final Practitioner practitioner = AttributionTestHelpers.createPractitionerResource("test-npi-1");
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final MethodOutcome outcome = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        final Practitioner pract2 = (Practitioner) outcome.getResource();

        // Assign it to the organization
        final PractitionerRole role = new PractitionerRole();
        role.setPractitioner(new Reference(pract2.getId()));
        role.setOrganization(new Reference(new IdType("Organization", DEFAULT_ORG_ID)));

        client
                .create()
                .resource(role)
                .encodedJson()
                .execute();

        // Try to fetch all the patients
        final Bundle providers = client
                .search()
                .forResource(Practitioner.class)
                .withTag("Organization", "Organization/" + AttributionTestHelpers.DEFAULT_ORG_ID)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        // We expect that the existing seeds already exist, plus the one we just added so this means 4 + 1 have been assigned to the organization
        assertEquals(5, providers.getEntry().size(), "Should have assigned providers");

        // Try to search for the provider, we should get the same results
        final Bundle searchedProviders = client
                .search()
                .forResource(Practitioner.class)
                .where(Patient.IDENTIFIER.exactly().identifier(pract2.getIdentifierFirstRep().getValue()))
                .withTag("Organization", "Organization/" + AttributionTestHelpers.DEFAULT_ORG_ID)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, searchedProviders.getEntry().size(), "Searched should be the same");
    }


    @Test
    void testPractitionerUpdate() {
        final Practitioner practitioner = AttributionTestHelpers.createPractitionerResource("test-npi-2");
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

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

}
