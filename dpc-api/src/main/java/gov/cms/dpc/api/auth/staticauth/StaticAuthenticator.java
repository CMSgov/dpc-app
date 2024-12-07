package gov.cms.dpc.api.auth.staticauth;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.Organization;

import com.google.inject.Inject;
import java.util.Optional;

/**
 * WARNING: DO NOT USE IN PRODUCTION
 * <p>
 * This {@link Authenticator} injects a test {@link Organization} by constructing a new value with the resource ID passed in as the credential value
 */
public class StaticAuthenticator implements Authenticator<DPCAuthCredentials, OrganizationPrincipal> {

    @Inject
    public StaticAuthenticator() {
        // do nothing because this is used for testing within FHIRSubmissionTest
    }

    @Override
    public Optional<OrganizationPrincipal> authenticate(DPCAuthCredentials credentials) {

        // Return a test organization

        return Optional.of(new OrganizationPrincipal(credentials.getOrganization()));
    }
}
