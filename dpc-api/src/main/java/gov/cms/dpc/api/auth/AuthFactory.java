package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthFilter;

/**
 * Interface for creating and injecting implementations of {@link AuthFilter}.
 */
public interface AuthFactory {

    AuthFilter<DPCAuthCredentials, OrganizationPrincipal> createPathAuthorizer(PathAuthorizer pa);

    AuthFilter<DPCAuthCredentials, OrganizationPrincipal> createStandardAuthorizer();
}
