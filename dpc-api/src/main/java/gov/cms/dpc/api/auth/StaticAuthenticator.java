package gov.cms.dpc.api.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import java.util.Optional;

/**
 * WARNING: DO NOT USE IN PRODUCTION
 * This always returns the same {@link Organization} for each request
 */
public class StaticAuthenticator implements Authenticator<String, OrganizationPrincipal> {

    private static final String ORGANIZATION_ID = "0c527d2e-2e8a-4808-b11d-0fa06baf8254";

    @Inject
    StaticAuthenticator() {
    }

    @Override
    public Optional<OrganizationPrincipal> authenticate(String credentials) throws AuthenticationException {
        // Return a test organization
        final Organization org = new Organization();
        org.setId(new IdType("Organization", ORGANIZATION_ID));
        return Optional.of(new OrganizationPrincipal(org));
    }
}
