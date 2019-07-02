package gov.cms.dpc.api.auth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.ResourceType;

import javax.inject.Inject;
import java.util.Optional;

public class MacaroonsAuthenticator implements Authenticator<String, OrganizationPrincipal> {

    private final IGenericClient client;

    @Inject
    MacaroonsAuthenticator(IGenericClient client) {
        this.client = client;
    }

    @Override
    public Optional<OrganizationPrincipal> authenticate(String credentials) throws AuthenticationException {
        // Try to search for an organization with the provided credential (token)
        final Bundle organizations = this.client
                .search()
                .forResource(Organization.class)
                .withTag("http://cms.gov/token", credentials)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        final Bundle.BundleEntryComponent organizationsEntryFirstRep = organizations.getEntryFirstRep();

        if (organizationsEntryFirstRep != null
                && organizationsEntryFirstRep.hasResource()
                && organizationsEntryFirstRep.getResource().getResourceType() == ResourceType.Organization) {

            return Optional.of(new OrganizationPrincipal((Organization) organizationsEntryFirstRep.getResource()));
        }

        return Optional.empty();
    }
}
