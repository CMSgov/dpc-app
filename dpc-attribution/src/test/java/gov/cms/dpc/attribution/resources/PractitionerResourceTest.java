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
import gov.cms.dpc.fhir.validations.profiles.PractitionerProfile;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.UriType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        assertThrows(InternalErrorException.class, creation::execute, "Should already exist");

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

        // We expect that the existing seeds already exist, plus the one we just added so this means 8 + 1 have been assigned to the organization
        assertEquals(9, providers.getEntry().size(), "Should have assigned providers");

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

        pract2.getNameFirstRep().setFamily("Updated");

        // Meta data doesn't persist, so update it again
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

        // Set the Meta data, so we know it deeply matches
        pract3.setMeta(meta);

        assertTrue(pract2.equalsDeep(pract3), "Updated values should match");
        assertFalse(pract3.equalsDeep(practitioner), "Should not match original");
    }

}
