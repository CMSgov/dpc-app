package gov.cms.dpc.api.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import java.util.Optional;

/**
 * WARNING: DO NOT USE IN PRODUCTION
 */
public class StaticAuthenticator implements Authenticator<String, OrganizationPrincipal> {

    @Inject
    StaticAuthenticator() {
    }

    @Override
    public Optional<OrganizationPrincipal> authenticate(String credentials) throws AuthenticationException {

        final Organization org = new Organization();
        org.setId("this-is-a-static-test");
        return Optional.of(new OrganizationPrincipal(org));
    }
}
