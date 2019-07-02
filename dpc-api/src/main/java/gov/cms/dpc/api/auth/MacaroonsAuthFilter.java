package gov.cms.dpc.api.auth;

import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import org.apache.http.HttpHeaders;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;

@Priority(Priorities.AUTHENTICATION)
@Provider
public class MacaroonsAuthFilter extends AuthFilter<String, OrganizationPrincipal> {

    private static final String BEARER_PREFIX = "Bearer";
    private static final String TOKEN_URI_PARAM = "token";

    @Inject
    MacaroonsAuthFilter(MacaroonsAuthorizer authorizer, MacaroonsAuthenticator authenticator) {
        this.authorizer = authorizer;
        this.authenticator = authenticator;

    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        // Try to get the
        String macaroon = getMacaroon(requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

        if (macaroon == null) {
            macaroon = requestContext.getUriInfo().getQueryParameters().getFirst(TOKEN_URI_PARAM);
        }

        if (!authenticate(requestContext, macaroon, SecurityContext.BASIC_AUTH)) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }
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
}
