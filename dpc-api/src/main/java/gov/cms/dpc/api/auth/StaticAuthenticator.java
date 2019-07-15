package gov.cms.dpc.api.auth;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;

import javax.inject.Inject;
import java.util.Optional;

/**
 * WARNING: DO NOT USE IN PRODUCTION
 * <p>
 * This {@link Authenticator} injects a test {@link Organization} by constructing a new value with the resource ID passed in as the credential value
 */
public class StaticAuthenticator implements Authenticator<String, OrganizationPrincipal> {


    @Inject
    StaticAuthenticator() {
    }

    @Override
    public Optional<OrganizationPrincipal> authenticate(String credentials) throws AuthenticationException {

        // Return a test organization
        final Organization org = new Organization();
        org.setId(new IdType("Organization", credentials));
        return Optional.of(new OrganizationPrincipal(org));
    }
}
