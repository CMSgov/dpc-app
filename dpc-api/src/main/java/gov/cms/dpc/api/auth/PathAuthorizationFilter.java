package gov.cms.dpc.api.auth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.common.hibernate.auth.DPCAuthManagedSessionFactory;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.UUID;


/**
 * Implementation of {@link DPCAuthFilter} that is used when a {@link PathAuthorizer} annotated method is called.
 * This extracts the Path value from the {@link UriInfo} and passes that value in the {@link DPCAuthCredentials} to the underlying {@link Authenticator}
 */
@Priority(Priorities.AUTHENTICATION)
public class PathAuthorizationFilter extends DPCAuthFilter {

    private static final Logger logger = LoggerFactory.getLogger(PathAuthorizationFilter.class);
    private final PathAuthorizer pa;

    PathAuthorizationFilter(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth, TokenDAO dao, PathAuthorizer pa) {
        super(bakery, auth, dao);
        this.pa = pa;
    }

    @Override
    protected DPCAuthCredentials buildCredentials(String macaroon, UUID organizationID, UriInfo uriInfo) {
        final String pathParam = this.pa.pathParam();
        final String pathValue = uriInfo.getPathParameters().getFirst(pathParam);
        if (pathValue == null) {
            logger.error("Cannot find path param {} on request. Has: {}", pathParam, uriInfo.getPathParameters().keySet());
            throw new WebApplicationException("Unable to get path parameter from request", Response.Status.INTERNAL_SERVER_ERROR);
        }

        final Organization organization = new Organization();
        organization.setId(new IdType("Organization", organizationID.toString()));
        return new DPCAuthCredentials(macaroon,
                organization,
                this.pa, pathValue);
    }
}
