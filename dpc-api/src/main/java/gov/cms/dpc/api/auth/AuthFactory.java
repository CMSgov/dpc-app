package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorized;
import io.dropwizard.auth.AuthFilter;

/**
 * Interface for creating and injecting implementations of {@link AuthFilter}.
 */
public interface AuthFactory {

    AuthFilter<DPCAuthCredentials, OrganizationPrincipal> createPathAuthorizer(PathAuthorized pa);

    AuthFilter<DPCAuthCredentials, OrganizationPrincipal> createAdminAuthorizer();

    AuthFilter<DPCAuthCredentials, OrganizationPrincipal> createStandardAuthorizer();
}
