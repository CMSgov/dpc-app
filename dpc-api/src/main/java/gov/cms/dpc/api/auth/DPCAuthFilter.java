package gov.cms.dpc.api.auth;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.jdbi.TokenDAO;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;
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


    protected DPCAuthFilter(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, OrganizationPrincipal> auth, TokenDAO dao) {
        this.authenticator = auth;
        this.bakery = bakery;
        this.dao = dao;
    }

    protected abstract DPCAuthCredentials buildCredentials(String macaroon, UUID organizationID, UriInfo uriInfo);

    @Override
    public void filter(final ContainerRequestContext requestContext) {
        final UriInfo uriInfo = requestContext.getUriInfo();
        final String macaroon = MacaroonHelpers.extractMacaroonFromRequest(requestContext, unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));

        // If we have a path authorizer, do that, otherwise, continue
        final DPCAuthCredentials dpcAuthCredentials = validateMacaroon(macaroon, uriInfo);

        final boolean authenticated = this.authenticate(requestContext, dpcAuthCredentials, null);
        if (!authenticated) {
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }
    }

    private DPCAuthCredentials validateMacaroon(String macaroon, UriInfo uriInfo) {

        logger.trace("Making request to validate token.");

        final List<Macaroon> m1;
        try {
            m1 = bakery.deserializeMacaroon(macaroon);
        } catch (BakeryException e) {
            logger.error("Cannot deserialize Macaroon", e);
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }

        // Lookup the organization by Macaroon id
        final UUID orgID = extractOrgIDFromMacaroon(m1);

        try {
            this.bakery.verifyMacaroon(m1, String.format("organization_id = %s", orgID));
        } catch (BakeryException e) {
            logger.error("Macaroon verification failed", e);
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
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
                throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
            }
            // Find the org_id caveat and extract the value
            final List<MacaroonCaveat> caveats = this.bakery.getCaveats(rootMacaroon);
            final MacaroonCondition orgCaveat = caveats
                    .stream()
                    .map(MacaroonCaveat::getCondition)
                    .filter(condition -> condition.getKey().equals("organization_id"))
                    .findAny()
                    .orElseThrow(() -> {
                        logger.error("Unable to get org from macaroon id", e);
                        return new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
                    });

            orgID = UUID.fromString(orgCaveat.getValue());
        }

        return orgID;
    }
}
