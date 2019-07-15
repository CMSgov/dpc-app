package gov.cms.dpc.api.auth;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.dpc.api.auth.annotations.PathAuthorizer;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import org.apache.http.HttpHeaders;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Organization;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
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
@Priority(Priorities.AUTHENTICATION)
public class MacaroonsAuthFilter extends DPCAuthFilter {

    private static final String BEARER_PREFIX = "Bearer";
    private static final String TOKEN_URI_PARAM = "token";

    private final IGenericClient client;
    private PathAuthorizer pa = null;

    @Inject
    MacaroonsAuthFilter(IGenericClient client, Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth) {
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
        final DPCAuthCredentials dpcAuthCredentials = validateMacaroon(macaroon, uriInfo);

        final boolean authenticated = this.authenticate(requestContext, dpcAuthCredentials, null);
        if (!authenticated) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }
    }

    @Override
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

    private DPCAuthCredentials validateMacaroon(String macaroon, UriInfo uriInfo) {


        // Make the request
        final Bundle returnedBundle = this

                .client
                .search()
                .forResource(Organization.class)
                .withTag("http://cms.gov/token", macaroon)
                .returnBundle(Bundle.class)
                .encodedJson()
                .execute();

        if (returnedBundle.getTotal() == 0) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }

        String pathValue = null;

        if (this.pa != null) {
            pathValue = uriInfo.getPathParameters().getFirst(this.pa.pathParam());
        }

        return new DPCAuthCredentials(macaroon,
                (Organization) returnedBundle.getEntryFirstRep().getResource(),
                this.pa, pathValue);
    }
}
