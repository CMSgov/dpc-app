package gov.cms.dpc.api.auth;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.common.MDCConstants;
import gov.cms.dpc.common.utils.XSSSanitizerUtil;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static gov.cms.dpc.api.auth.MacaroonHelpers.BEARER_PREFIX;

/**
 * {@link AuthFilter} implementation which extracts the Macaroon (base64 encoded) from the request.
 * Once extracted, it passes it down along the authn/authz chain.
 * <p>
 * This assumes that the Macaroon is either passed via the {@link HttpHeaders#AUTHORIZATION} header
 * in the form 'Bearer {macaroon-values}'.
 * <p>
 * Or, directly via the 'token' query param (e.g. no Bearer prefix)
 */
public abstract class DPCAuthFilter extends AuthFilter<DPCAuthCredentials, OrganizationPrincipal> {

    private static final Logger logger = LoggerFactory.getLogger(DPCAuthFilter.class);


    private final TokenDAO dao;
    private final MacaroonBakery bakery;
    private final DPCUnauthorizedHandler dpc401handler;


    protected DPCAuthFilter(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth, TokenDAO dao, DPCUnauthorizedHandler dpc401handler ) {
        this.authenticator = auth;
        this.bakery = bakery;
        this.dao = dao;
        this.dpc401handler = dpc401handler;
    }

    protected abstract DPCAuthCredentials buildCredentials(String macaroon, UUID organizationID, UriInfo uriInfo);

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        final UriInfo uriInfo = requestContext.getUriInfo();
        final String macaroon = MacaroonHelpers.extractMacaroonFromRequest(requestContext, unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));

        final DPCAuthCredentials dpcAuthCredentials = validateMacaroon(macaroon, uriInfo);
        final String orgId = dpcAuthCredentials.getOrganization().getId();
        final String resourceRequested = XSSSanitizerUtil.sanitize(uriInfo.getPath());
        final String method = requestContext.getMethod();

        // TODO Remove this when we want to turn on the IpAddress end point
        if(resourceRequested.equals("v1/IpAddress")) {
            throw new WebApplicationException(Response.Status.FORBIDDEN);
        }

        final boolean authenticated = this.authenticate(requestContext, dpcAuthCredentials, null);
        if (!authenticated) {
            throw new WebApplicationException(dpc401handler.buildResponse(BEARER_PREFIX, realm));
        }
        logger.info("event_type=request-received, resource_requested={}, organization_id={}, method={}", resourceRequested, orgId, method);
    }

    private DPCAuthCredentials validateMacaroon(String macaroon, UriInfo uriInfo) {

        logger.trace("Making request to validate token.");

        final List<Macaroon> m1;
        try {
            m1 = MacaroonBakery.deserializeMacaroon(macaroon);
        } catch (BakeryException e) {
            logger.error("Cannot deserialize Macaroon", e);
            throw new WebApplicationException(dpc401handler.buildResponse(BEARER_PREFIX, realm));
        }

        // Lookup the organization by Macaroon id
        final UUID orgID = extractOrgIDFromMacaroon(m1);

        // Now that we have the organization_id, set it in the logging context

        MDC.put(MDCConstants.ORGANIZATION_ID, orgID.toString());
        MDC.put(MDCConstants.TOKEN_ID, m1.get(0).identifier);

        try {
            this.bakery.verifyMacaroon(m1, String.format("organization_id = %s", orgID));
        } catch (BakeryException e) {
            logger.error("Macaroon verification failed", e);
            throw new WebApplicationException(dpc401handler.buildResponse(BEARER_PREFIX, realm));
        }

        return buildCredentials(macaroon, orgID, uriInfo);
    }

    private UUID extractOrgIDFromMacaroon(List<Macaroon> macaroons) {
        final Macaroon rootMacaroon = macaroons.get(0);
        final UUID macaroonID = UUID.fromString(rootMacaroon.identifier);
        UUID orgID;
        try {
            orgID = this.dao.findOrgByToken(macaroonID);
        } catch (Exception e) {
            // The macaroon ID doesn't match, we need to determine if we're looking at a Golden Macaroon, or if the client id has been deleted
            // Check the length of the provided Macaroons, if more than 1, it's a client token which has been removed, so fail
            // If the length is 1 it's either a golden macaroon or an undischarged Macaroon, which will fail in the next auth phase
            if (macaroons.size() > 1) {
                throw new WebApplicationException(dpc401handler.buildResponse(BEARER_PREFIX, realm));
            }
            // Find the org_id caveat and extract the value
            orgID = MacaroonHelpers.extractOrgIDFromCaveats(Collections.singletonList(rootMacaroon))
                    .orElseThrow(() -> {
                        logger.error("Cannot find organization_id on Macaroon");
                        throw new WebApplicationException(dpc401handler.buildResponse(BEARER_PREFIX, realm));
                    });
        }

        return orgID;
    }
}
