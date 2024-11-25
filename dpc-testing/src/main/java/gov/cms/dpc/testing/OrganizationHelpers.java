package gov.cms.dpc.testing;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Parameters;

import java.io.InputStream;

/**
 * Helper class for holding common {@link org.hl7.fhir.dstu3.model.Organization} helper methods
 */
public class OrganizationHelpers {

    private OrganizationHelpers() {
        // Not used
    }

    /**
     * Create an organization by calling the $submit operation on the {@link Organization} endpoint
     *
     * @param ctx    - {@link FhirContext} to use for deserializing JSON resources
     * @param client - {@link IGenericClient} for actually making the API call
     * @return - newly minted {@link Organization}
     */
    public static Organization createOrganization(FhirContext ctx, IGenericClient client) {
        return createOrganization(ctx, client, "1334567892", false);
    }

    /**
     * Create an organization by calling the $submit operation on the {@link Organization} endpoint
     *
     * @param ctx             - {@link FhirContext} to use for deserializing JSON resources
     * @param client          - {@link IGenericClient} for actually making the API call
     * @param organizationNPI - {@link String} specific NPI to use for test
     * @param skipExists      - {@code true} don't check to see if Org exists before creating. {@code false} check for existence
     * @return - newly minted {@link Organization}
     */
    public static Organization createOrganization(FhirContext ctx, IGenericClient client, String organizationNPI, boolean skipExists) {

        // Check to see if the organization already exists, otherwise, create it
        if (!skipExists) {
            final Bundle searchBundle = client
                    .search()
                    .forResource(Organization.class)
                    .where(Organization.IDENTIFIER.exactly().systemAndCode("http://hl7.org/fhir/sid/us-npi", "1111111211"))
                    .returnBundle(Bundle.class)
                    .encodedJson()
                    .execute();

            if (searchBundle.getTotal() > 0) {
                return (Organization) searchBundle.getEntryFirstRep().getResource();
            }
        }

        // Read in the test file
        final InputStream inputStream = OrganizationHelpers.class.getClassLoader().getResourceAsStream("organization.tmpl.json");
        final Bundle resource = (Bundle) ctx.newJsonParser().parseResource(inputStream);

        // Manually update the NPI
        ((Organization) resource.getEntryFirstRep().getResource()).getIdentifierFirstRep().setValue(organizationNPI);

        final Parameters parameters = new Parameters();
        parameters.addParameter().setResource(resource).setName("resource");

        return client
                .operation()
                .onType(Organization.class)
                .named("submit")
                .withParameters(parameters)
                .returnResourceType(Organization.class)
                .encodedJson()
                .execute();
    }
}
