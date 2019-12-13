package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.gclient.IDeleteTyped;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import gov.cms.dpc.attribution.AttributionTestHelpers;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRExtractors;
import gov.cms.dpc.fhir.validations.profiles.PractitionerProfile;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static gov.cms.dpc.attribution.AttributionTestHelpers.DEFAULT_ORG_ID;
import static gov.cms.dpc.attribution.AttributionTestHelpers.createFHIRClient;
import static gov.cms.dpc.common.utils.SeedProcessor.createBaseAttributionGroup;
import static org.junit.jupiter.api.Assertions.*;

class PractitionerResourceTest extends AbstractAttributionTest {

    private PractitionerResourceTest() {
        // Not used
    }

    @Test
    void testPractitionerReadWrite() {

        final Practitioner practitioner = AttributionTestHelpers.createPractitionerResource("test-npi-1");

        final IGenericClient client = createFHIRClient(ctx, getServerURL());
        final ICreateTyped creation = client
                .create()
                .resource(practitioner)
                .encodedJson();
        final MethodOutcome mo = creation
                .execute();

        final Practitioner pract2 = (Practitioner) mo.getResource();

        // Verify that it has the correct profile
        // Find the correct profile
        final boolean foundProfile = pract2
                .getMeta()
                .getProfile()
                .stream()
                .map(UriType::getValueAsString)
                .anyMatch(profileString -> profileString.equals(PractitionerProfile.PROFILE_URI));

        assertTrue(foundProfile, "Should have appropriate DPC profile");

        // Try again, should fail
        final MethodOutcome execute = creation.execute();
        assertNull(execute.getCreated(), "Should already exist");

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


        // Try to fetch all the patients
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("organization", Collections.singletonList(DEFAULT_ORG_ID));
        final Bundle providers = client
                .search()
                .forResource(Practitioner.class)
                .whereMap(searchParams)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(5, providers.getEntry().size(), "Should have assigned providers");

        searchParams.put("identifier", Collections.singletonList(pract2.getIdentifierFirstRep().getValue()));
        // Try to search for the provider, we should get the same results
        final Bundle searchedProviders = client
                .search()
                .forResource(Practitioner.class)
                .whereMap(searchParams)
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

        // Get the updated time
        final Date createdAt = pract2.getMeta().getLastUpdated();

        pract2.getNameFirstRep().setFamily("Updated");

        // Reset the metadata to wipe out the timestamps
        final Meta meta = new Meta();
        meta.addTag(DPCIdentifierSystem.DPC.getSystem(), DEFAULT_ORG_ID, "Organization ID");
        pract2.setMeta(meta);

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

        final Date updatedAt = pract3.getMeta().getLastUpdated();

        // Set the Meta data, so we know it deeply matches
        pract3.setMeta(meta);

        assertAll(() -> assertTrue(pract2.equalsDeep(pract3), "Updated values should match"),
                () -> assertFalse(pract3.equalsDeep(practitioner), "Should not match original"),
                () -> assertTrue(createdAt.before(updatedAt), "Creation should be before updated"));
    }

    @Test
    void testPractitionerRemoval() {
        final Practitioner practitioner = AttributionTestHelpers.createPractitionerResource("test-npi-2");
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final MethodOutcome outcome = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        final Practitioner pract2 = (Practitioner) outcome.getResource();

        // Add an attribution Group
        final Group group = createBaseAttributionGroup(FHIRExtractors.getProviderNPI(practitioner), DEFAULT_ORG_ID);

        client
                .create()
                .resource(group)
                .encodedJson()
                .execute();

        // Now remove the practitioner

        client
                .delete()
                .resourceById("Practitioner", pract2.getIdElement().getIdPart())
                .encodedJson()
                .execute();

        // Ensure it's gone
        final IReadExecutable<Practitioner> getRequest = client
                .read()
                .resource(Practitioner.class)
                .withId(pract2.getId())
                .encodedJson();

        assertThrows(ResourceNotFoundException.class, getRequest::execute, "Should not have resource");
    }

}
