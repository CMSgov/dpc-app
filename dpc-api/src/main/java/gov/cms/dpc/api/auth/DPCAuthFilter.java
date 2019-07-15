package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthFilter;

/**
 * Wrapper class around {@link AuthFilter} which is specific to DPC and allows us to persist the {@link PathAuthorizer} annotation from the request resource
 */
abstract class DPCAuthFilter extends AuthFilter<DPCAuthCredentials, OrganizationPrincipal> {

    abstract void setPathAuthorizer(PathAuthorizer pa);
}
