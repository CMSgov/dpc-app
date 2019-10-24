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

    /**
     * Create an organization by calling the $submit operation on the {@link Organization} endpoint
     *
     * @param ctx    - {@link FhirContext} to use for deserializing JSON resources
     * @param client - {@link IGenericClient} for actually making the API call
     * @return - newly minted {@link Organization}
     */
    public static Organization createOrganization(FhirContext ctx, IGenericClient client) {
        return createOrganization(ctx, client, "test-org-npi");
    }

    /**
     * Create an organization by calling the $submit operation on the {@link Organization} endpoint
     *
     * @param ctx             - {@link FhirContext} to use for deserializing JSON resources
     * @param client          - {@link IGenericClient} for actually making the API call
     * @param organizationNPI - {@link String} specific NPI to use for test
     * @return - newly minted {@link Organization}
     */
    public static Organization createOrganization(FhirContext ctx, IGenericClient client, String organizationNPI) {

//         Check to see if the organization already exists, otherwise, create it
//        final Bundle searchBundle = client
//                .search()
//                .forResource(Organization.class)
//                .where(Organization.IDENTIFIER.exactly().systemAndCode("http://hl7.org/fhir/sid/us-npi", "test-org-npi"))
//                .returnBundle(Bundle.class)
//                .encodedJson()
//                .execute();
//
//        if (searchBundle.getTotal() > 0) {
//            return (Organization) searchBundle.getEntryFirstRep().getResource();
//        }

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
