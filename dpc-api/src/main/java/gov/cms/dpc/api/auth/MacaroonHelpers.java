package gov.cms.dpc.api.auth;

import org.apache.http.HttpHeaders;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public class MacaroonHelpers {

    private MacaroonHelpers() {
        // Not used
    }

    public static final String BEARER_PREFIX = "Bearer";
    static final String TOKEN_URI_PARAM = "token";

    public static String extractMacaroonFromRequest(ContainerRequestContext requestContext, Response unauthorizedResponse) {
        // Try to get the Macaroon from the request
        String macaroon = getMacaroon(requestContext.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));

        final UriInfo uriInfo = requestContext.getUriInfo();
        if (macaroon == null) {
            macaroon = uriInfo.getQueryParameters().getFirst(TOKEN_URI_PARAM);
        }

        if (macaroon == null) {
            throw new WebApplicationException(unauthorizedResponse);
        }

        return macaroon;
    }

    @Nullable
    private static String getMacaroon(String header) {
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
