package gov.cms.dpc.api.auth;

import io.dropwizard.auth.AuthFilter;
import org.apache.http.HttpHeaders;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;

public class MacaroonsAuthFilter<P extends Principal> extends AuthFilter<String, P> {

    private static final String BEARER_PREFIX = "Bearer";
    private static final String TOKEN_URI_PARAM = "token";

    private MacaroonsAuthFilter() {
        // Not used
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
