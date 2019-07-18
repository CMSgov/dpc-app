package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.ICreateTyped;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.UUID;

import static gov.cms.dpc.attribution.AttributionTestHelpers.createFHIRClient;
import static gov.cms.dpc.attribution.AttributionTestHelpers.createPractitionerResource;
import static org.junit.jupiter.api.Assertions.*;

class PractitionerRoleTest extends AbstractAttributionTest {

    private final IGenericClient client;

    private PractitionerRoleTest() {
        this.client = createFHIRClient(ctx, getServerURL());
    }

    @Test
    void testPractitionerRoleReadWrite() {

        // Create the practitioner and Organizations
        final String providerID = persistProvider("test-npi-1");
        final String organizationID = persistOrganization();

        // Now we can create things
        final PractitionerRole createdRole = persistRole(organizationID, providerID);

        final PractitionerRole fetchedRole = client
                .read()
                .resource(PractitionerRole.class)
                .withId(createdRole.getId())
                .encodedJson()
                .execute();

        assertTrue(createdRole.equalsDeep(fetchedRole), "Should be equal");

        // Now delete it
        client
                .delete()
                .resourceById(fetchedRole.getIdElement())
                .encodedJson()
                .execute();

        // Try to fetch it, which should fail
        final var missingFetch = client
                .read()
                .resource(PractitionerRole.class)
                .withId(fetchedRole.getId())
                .encodedJson();

        assertThrows(ResourceNotFoundException.class, missingFetch::execute, "Should not find role");
    }

    @Test
    void testMissingOrganization() {
        final String providerID = persistProvider("test-npi-2");

        // Create a role with a missing Organization
        final Organization organization = new Organization();
        organization.setId(new IdType("Organization", UUID.randomUUID().toString()));
        organization.addIdentifier().setValue("fake-org-npi");
        final PractitionerRole role = new PractitionerRole();

        role.setPractitioner(new Reference(providerID));
        role.setOrganization(new Reference(organization.getId()));

        final ICreateTyped creation = client
                .create()
                .resource(role)
                .encodedJson();
        assertThrows(InternalErrorException.class, creation::execute, "Should throw a not found exception");
    }

    @Test
    void testMissingProvider() {
        final String organizationID = persistOrganization();

        // Fake provider
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue("not-a-real-provider");
        practitioner.setId(new IdType("Practitioner", UUID.randomUUID().toString()));

        final PractitionerRole role = new PractitionerRole();

        role.setPractitioner(new Reference(practitioner.getId()));
        role.setOrganization(new Reference(organizationID));

        final ICreateTyped creation = client
                .create()
                .resource(role)
                .encodedJson();
        assertThrows(InternalErrorException.class, creation::execute, "Should throw a not found exception");
    }

    @Test
    void testOrganizationSearch() {
        // Do all the things
        final String organizationID = persistOrganization();
        final String providerID = persistProvider("yet-another-one");
        persistRole(organizationID, providerID);

        final Bundle roleBundle = client
                .search()
                .forResource(PractitionerRole.class)
                .where(PractitionerRole.ORGANIZATION.hasId(organizationID))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertEquals(1, roleBundle.getTotal(), "Should have a single role");
    }

    @Test
    void testProviderSearch() {
        final String organizationID = persistOrganization();
        final String providerID = persistProvider("yet-another-one-2");
        final PractitionerRole role = persistRole(organizationID, providerID);

        final Bundle roleBundle = client
                .search()
                .forResource(PractitionerRole.class)
                .where(PractitionerRole.PRACTITIONER.hasId(providerID))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        assertAll(() -> assertEquals(1, roleBundle.getTotal(), "Should have a single role"),
                () -> assertTrue(role.equalsDeep(roleBundle.getEntryFirstRep().getResource()), "Roles should be equal"));
    }

    private String persistOrganization() {
        final InputStream inputStream = OrganizationResourceTest.class.getClassLoader().getResourceAsStream("organization.tmpl.json");
        final Bundle resource = (Bundle) ctx.newJsonParser().parseResource(inputStream);

        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(resource);

        client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .encodedJson()
                .execute();

        // Figure out its ID
        final Bundle bundle = client
                .search()
                .forResource(Organization.class)
                .where(Practitioner.IDENTIFIER.exactly().identifier("test-org-npi"))
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        final Organization organization = (Organization) bundle.getEntryFirstRep().getResource();

        return organization.getId();
    }

    private String persistProvider(String npi) {
        final Practitioner practitioner = createPractitionerResource(npi);

        final MethodOutcome outcome = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        final Practitioner persistedPractitioner = (Practitioner) outcome.getResource();
        return persistedPractitioner.getId();
    }

    private PractitionerRole persistRole(String organizationID, String providerID) {
        final PractitionerRole role = new PractitionerRole();

        role.setPractitioner(new Reference(providerID));
        role.setOrganization(new Reference(organizationID));

        final MethodOutcome roleOutcome = client
                .create()
                .resource(role)
                .encodedJson()
                .execute();

        return (PractitionerRole) roleOutcome.getResource();
    }
}
