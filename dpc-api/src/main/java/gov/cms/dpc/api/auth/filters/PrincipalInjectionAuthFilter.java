package gov.cms.dpc.api.auth.filters;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.DPCAuthFilter;
import gov.cms.dpc.api.auth.DPCUnauthorizedHandler;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.Authenticator;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.UriInfo;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;

import java.util.UUID;

/**
 * Implementation of {@link DPCAuthFilter} that is use when an {@link io.dropwizard.auth.Auth} annotated method is called.
 * This simply passes the {@link Organization} to the method and assumes that it handles all of the necessary security controls and such.
 */
@Priority(Priorities.AUTHENTICATION)
public class PrincipalInjectionAuthFilter extends DPCAuthFilter {

    public PrincipalInjectionAuthFilter(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth, TokenDAO dao, DPCUnauthorizedHandler dpc401handler) {
        super(bakery, auth, dao, dpc401handler);
    }

    @Override
    protected DPCAuthCredentials buildCredentials(String macaroon, UUID organizationID, UriInfo uriInfo) {
        final Organization resource = new Organization();
        resource.setId(new IdType("Organization", organizationID.toString()));
        return new DPCAuthCredentials(macaroon, resource, null, null);
    }
}
