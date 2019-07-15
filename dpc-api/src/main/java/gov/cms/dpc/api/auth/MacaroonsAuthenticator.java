package gov.cms.dpc.api.auth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import java.util.*;

/**
 * Implementation of {@link Authenticator} which matches an {@link Organization} to the given Macaroon (base64 encoded string
 * If no {@link Organization} is found, this returns an empty optional, which signifies and authorization failure.
 */
public class MacaroonsAuthenticator implements Authenticator<DPCAuthCredentials, OrganizationPrincipal> {

    private final IGenericClient client;

    @Inject
    MacaroonsAuthenticator(IGenericClient client) {
        this.client = client;
    }

    @Override
    public Optional<OrganizationPrincipal> authenticate(DPCAuthCredentials credentials) throws AuthenticationException {

        // If we don't have a path authorizer, just return the principal
        final OrganizationPrincipal principal = new OrganizationPrincipal(credentials.getOrganization());
        if (credentials.getPathAuthorizer() == null) {
            return Optional.of(principal);
        }

        // Now, try to lookup the matching resource
        Map<String, List<String>> searchParams = new HashMap<>();
        searchParams.put("identifier", Collections.singletonList(credentials.getPathValue()));
        final Bundle bundle = this.client
                .search()
                .forResource(credentials.getPathAuthorizer().type().getPath())
                .whereMap(searchParams)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (bundle.getTotal() == 0) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }
}
