package gov.cms.dpc.api.auth.staticauth;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import io.dropwizard.auth.Authenticator;
import jakarta.inject.Inject;

import java.security.Principal;
import java.util.Optional;

/**
 * WARNING: DO NOT USE IN PRODUCTION
 * <p>
 * This {@link Authenticator} injects an ID-only Organization of the requested API version.
 */
public class StaticAuthenticator implements Authenticator<DPCAuthCredentials, Principal> {

    @Inject
    public StaticAuthenticator() {
        // do nothing because this is used for testing within FHIRSubmissionTest
    }

    @Override
    public Optional<Principal> authenticate(DPCAuthCredentials credentials) {
        if ("v1".equals(credentials.getApiVersion())) {
            final org.hl7.fhir.dstu3.model.Organization organization = new org.hl7.fhir.dstu3.model.Organization();
            organization.setId(new org.hl7.fhir.dstu3.model.IdType("Organization", credentials.getOrganizationId()));
            return Optional.of(new OrganizationPrincipal(organization));
        }
        if ("v3".equals(credentials.getApiVersion())) {
            final org.hl7.fhir.r4.model.Organization organization = new org.hl7.fhir.r4.model.Organization();
            organization.setId(new org.hl7.fhir.r4.model.IdType("Organization", credentials.getOrganizationId()));
            return Optional.of(new gov.cms.dpc.api.auth.r4.OrganizationPrincipal(organization));
        }
        throw new IllegalStateException("No static authenticator configured for API version: " + credentials.getApiVersion());
    }
}
