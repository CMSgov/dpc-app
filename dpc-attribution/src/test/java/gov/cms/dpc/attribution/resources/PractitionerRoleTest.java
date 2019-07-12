package gov.cms.dpc.attribution.resources;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.attribution.AbstractAttributionTest;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static gov.cms.dpc.attribution.AttributionTestHelpers.createFHIRClient;
import static gov.cms.dpc.attribution.AttributionTestHelpers.createPractitionerResource;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PractitionerRoleTest extends AbstractAttributionTest {

    private PractitionerRoleTest() {
        // // Not used
    }

    @Test
    void testPractitionerRoleReadWrite() {

        // Create the practitioner and Organizations
        final Practitioner practitioner = createPractitionerResource("test-npi-1");
        final IGenericClient client = createFHIRClient(ctx, getServerURL());

        final MethodOutcome outcome = client
                .create()
                .resource(practitioner)
                .encodedJson()
                .execute();

        final Practitioner persistedPractitioner = (Practitioner) outcome.getResource();

        // Now the Organization
        // Read in the test file
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

        // Now we can create things

        final PractitionerRole role = new PractitionerRole();

        role.setPractitioner(new Reference(persistedPractitioner.getId()));
        role.setOrganization(new Reference(organization.getId()));

        final MethodOutcome roleOutcome = client
                .create()
                .resource(role)
                .encodedJson()
                .execute();

        final PractitionerRole createdRole = (PractitionerRole) roleOutcome.getResource();

        final PractitionerRole fetchedRole = client
                .read()
                .resource(PractitionerRole.class)
                .withId(createdRole.getId())
                .encodedJson()
                .execute();

        assertTrue(createdRole.equalsDeep(fetchedRole), "Should be equal");
    }

    @Test
    void testMissingOrganization() {

    }

    @Test
    void testMissingProvider() {

    }

    @Test
    void testOrganizationSearch() {

    }

    @Test
    void testProviderSearch() {

    }
}
