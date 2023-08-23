package gov.cms.dpc.api.auth.filters;

import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.DPCAuthFilter;
import gov.cms.dpc.api.auth.DPCUnauthorizedHandler;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.common.utils.XSSSanitizerUtil;
import gov.cms.dpc.macaroons.MacaroonBakery;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.inject.Inject;
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

    @Inject
    public PathAuthorizationFilter(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth, TokenDAO dao, PathAuthorizer pa, DPCUnauthorizedHandler dpc401handler) {
        super(bakery, auth, dao, dpc401handler);
        this.pa = pa;
    }

    @Override
    protected DPCAuthCredentials buildCredentials(String macaroon, UUID organizationID, UriInfo uriInfo) {
        final String pathParam = this.pa.pathParam();
        final String pathValue = uriInfo.getPathParameters().getFirst(pathParam);
        if (pathValue == null) {
            logger.error("Cannot find path param {} on request. Has: {}", XSSSanitizerUtil.sanitize(pathParam), uriInfo.getPathParameters().keySet());
            throw new WebApplicationException("Unable to get path parameter from request", Response.Status.INTERNAL_SERVER_ERROR);
        }

        final Organization organization = new Organization();
        organization.setId(new IdType("Organization", organizationID.toString()));
        return new DPCAuthCredentials(macaroon,
                organization,
                this.pa, pathValue);
    }
}
