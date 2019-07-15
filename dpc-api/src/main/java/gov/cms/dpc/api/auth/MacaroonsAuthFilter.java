package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.annotations.AttributionService;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import org.apache.http.HttpHeaders;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
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
public class MacaroonsAuthFilter extends DPCAuthFilter {

    private static final String BEARER_PREFIX = "Bearer";
    private static final String TOKEN_URI_PARAM = "token";

    private final WebTarget client;
    private PathAuthorizer pa = null;

    @Inject
    MacaroonsAuthFilter(@AttributionService WebTarget client, Authenticator<String, OrganizationPrincipal> auth) {
        this.client = client;
        this.authenticator = auth;
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        // Try to get the Macaroon from the request
        String macaroon = getMacaroon(requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

        final UriInfo uriInfo = requestContext.getUriInfo();

        if (macaroon == null) {
            macaroon = uriInfo.getQueryParameters().getFirst(TOKEN_URI_PARAM);
        }

        if (macaroon == null) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }

        // If we have a path authorizer, do that, otherwise, continue
        if (pa != null) {
            validatePath(this.pa, macaroon, uriInfo);
        }

        this.authenticate(requestContext, macaroon, null);
    }

    void setPathAuthorizer(PathAuthorizer authorizer) {
        this.pa = authorizer;
    }

    @Nullable
    private String getMacaroon(String header) {
        if (header == null) {
            return null;
        }

        final int space = header.indexOf(' ');
        if (space <= 0) {
            return null;
        }

        final String method = header.substring(0, space);
        if (!BEARER_PREFIX.equalsIgnoreCase(method)) {
            return null;
        }

        return header.substring(space + 1);
    }

    private void validatePath(PathAuthorizer pa, String macaroon, UriInfo uriInfo) {

        final String pathValue = uriInfo.getPathParameters().getFirst(this.pa.pathParam());
        if (pathValue == null) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }
        // Make the request
        final Response tokenValid = this.client
                .path(String.format("%s/%s/token/verify", this.pa.type().toString(), pathValue))
                .queryParam("token", macaroon)
                .request(FHIRMediaTypes.FHIR_JSON)
                .buildGet()
                .invoke();

        if (tokenValid.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }
    }
}
