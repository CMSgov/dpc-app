package gov.cms.dpc.api.auth;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.Organization;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static gov.cms.dpc.api.auth.AuthHelpers.BEARER_PREFIX;

@Priority(Priorities.AUTHENTICATION)
public class AdminAuthFilter extends AuthFilter<DPCAuthCredentials, OrganizationPrincipal> {

    private final MacaroonBakery bakery;

    AdminAuthFilter(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, OrganizationPrincipal> authenticator) {
        this.authenticator = authenticator;
        this.bakery = bakery;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final UriInfo uriInfo = requestContext.getUriInfo();
        final String macaroon = AuthHelpers.extractMacaroonFromRequest(requestContext, unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));

        // Validate Macaroon
        final Macaroon m1;
        try {
            m1 = bakery.deserializeMacaroon(macaroon);
        } catch (BakeryException e) {
            logger.error("Cannot deserialize Macaroon", e);
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }

        try {
            this.bakery.verifyMacaroon(Collections.singletonList(m1));
        } catch (BakeryException e) {
            logger.error("Macaroon verification failed", e);
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }

        // Ensure that we don't have any organization IDs
        // Since we ALWAYS generate organization_id caveats for tokens, its absence indicates that its a Golden Macaroon
        final boolean isGoldenMacaroon = this.bakery.getCaveats(m1)
                .stream()
                .map(MacaroonCaveat::getCondition)
                .anyMatch(cond -> cond.getKey().equals("organization_id"));

        if (isGoldenMacaroon) {
            logger.error("Attempted to call Admin endpoint with Organization token");
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }
        this.authenticate(requestContext, new DPCAuthCredentials(macaroon, new Organization(), null, null), null);
    }
}
