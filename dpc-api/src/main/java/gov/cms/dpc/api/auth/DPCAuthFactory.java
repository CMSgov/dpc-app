package gov.cms.dpc.api.auth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.Authenticator;

import javax.inject.Inject;

public class DPCAuthFactory implements AuthFactory {

    private final IGenericClient client;
    private final Authenticator<DPCAuthCredentials, OrganizationPrincipal> authenticator;

    @Inject
    public DPCAuthFactory(IGenericClient client, Authenticator<DPCAuthCredentials, OrganizationPrincipal> authenticator) {
        this.client = client;
        this.authenticator = authenticator;
    }

    @Override
    public DPCAuthFilter createPathAuthorizer(PathAuthorizer pa) {
        return new PathAuthorizationFilter(client, authenticator, pa);
    }

    @Override
    public DPCAuthFilter createStandardAuthorizer() {
        return new PrincipalInjectionAuthFilter(client, authenticator);
    }
}
