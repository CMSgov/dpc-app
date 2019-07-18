package gov.cms.dpc.api.auth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import org.apache.http.HttpHeaders;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
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
abstract class DPCAuthFilter extends AuthFilter<DPCAuthCredentials, OrganizationPrincipal> {

    private static final String BEARER_PREFIX = "Bearer";
    private static final String TOKEN_URI_PARAM = "token";
    private static final Logger logger = LoggerFactory.getLogger(DPCAuthFilter.class);

    private final IGenericClient client;


    DPCAuthFilter(IGenericClient client, Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth) {
        this.client = client;
        this.authenticator = auth;
    }

    protected abstract DPCAuthCredentials buildCredentials(String macaroon, Organization resource, UriInfo uriInfo);

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
        final DPCAuthCredentials dpcAuthCredentials = validateMacaroon(macaroon, uriInfo);

        final boolean authenticated = this.authenticate(requestContext, dpcAuthCredentials, null);
        if (!authenticated) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }
    }

    private DPCAuthCredentials validateMacaroon(String macaroon, UriInfo uriInfo) {

        logger.warn("Making request to validate token.");
        final Bundle returnedBundle = this
                .client
                .search()
                .forResource(Organization.class)
                .withTag("http://cms.gov/token", macaroon)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        logger.warn("Found {} matching organizations", returnedBundle.getTotal());
        if (returnedBundle.getTotal() == 0) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }

        return buildCredentials(macaroon, (Organization) returnedBundle.getEntryFirstRep().getResource(), uriInfo);
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
