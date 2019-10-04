package gov.cms.dpc.api.auth;

import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;

/**
 * Implementation of {@link DPCAuthFilter} that is use when an {@link io.dropwizard.auth.Auth} annotated method is called.
 * This simply passes the {@link Organization} to the method and assumes that it handles all of the necessary security controls and such.
 */
@Priority(Priorities.AUTHENTICATION)
public class PrincipalInjectionAuthFilter extends DPCAuthFilter {

    PrincipalInjectionAuthFilter(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth, DPCAuthManagedSessionFactory factory) {
        super(bakery, auth, factory);
    }

    @Override
    protected DPCAuthCredentials buildCredentials(String macaroon, UUID organizationID, UriInfo uriInfo) {
        final Organization resource = new Organization();
        resource.setId(new IdType("Organization", organizationID.toString()));
        return new DPCAuthCredentials(macaroon, resource, null, null);
    }
}
