package gov.cms.dpc.api.auth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.Organization;

import javax.ws.rs.core.UriInfo;

public class PrincipalInjectionAuthFilter extends DPCAuthFilter {

    PrincipalInjectionAuthFilter(IGenericClient client, Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth) {
        super(client, auth);
    }

    @Override
    protected DPCAuthCredentials buildCredentials(String macaroon, Organization resource, UriInfo uriInfo) {
        return new DPCAuthCredentials(macaroon, resource, null, null);
    }
}
