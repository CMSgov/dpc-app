package gov.cms.dpc.api.auth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import org.apache.http.HttpHeaders;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;

/**
 * {@link AuthFilter} implementation which extracts the Macaroon (base64 encoded) from the request.
 * Once extracted, it passes it down along the authn/authz chain.
 * <p>
 * This assumes that the Macaroon is either passed via the {@link HttpHeaders#AUTHORIZATION} header
 * in the form 'Bearer {macaroon-values}'.
 * <p>
 * Or, directly via the 'token' query param (e.g. no Bearer prefix)
 */
@Priority(Priorities.AUTHENTICATION)
public class PathAuthorizationFilter extends DPCAuthFilter {

    private static final Logger logger = LoggerFactory.getLogger(PathAuthorizationFilter.class);
    private final PathAuthorizer pa;

    PathAuthorizationFilter(IGenericClient client, Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth, PathAuthorizer pa) {
        super(client, auth);
        logger.warn("CONSTRUCTING");
        this.pa = pa;
    }

    @Override
    protected DPCAuthCredentials buildCredentials(String macaroon, Organization resource, UriInfo uriInfo) {
        final String pathParam = this.pa.pathParam();
        final String pathValue = uriInfo.getPathParameters().getFirst(pathParam);
        if (pathValue == null) {
            logger.error("Cannot find path param {} on request. Has: {}", pathParam, uriInfo.getPathParameters().keySet());
            throw new WebApplicationException("Unable to get path parameter from request", Response.Status.INTERNAL_SERVER_ERROR);
        }
        return new DPCAuthCredentials(macaroon,
                resource,
                this.pa, pathValue);
    }
}
