package gov.cms.dpc.api.auth.filters;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.api.auth.DPCAuthCredentials;
import gov.cms.dpc.api.auth.DPCAuthFilter;
import gov.cms.dpc.api.auth.MacaroonHelpers;
import gov.cms.dpc.api.auth.OrganizationPrincipal;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.Authenticator;
import org.hl7.fhir.dstu3.model.Organization;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import java.util.List;

import static gov.cms.dpc.api.auth.MacaroonHelpers.BEARER_PREFIX;

/**
 * Implementation of {@link AuthFilter} to use when an {@link gov.cms.dpc.api.auth.annotations.AdminOperation} annotated method (or class) is called.
 * This method does not inherit from {@link DPCAuthFilter} and is designed to ensure that Organization access tokens cannot be passed to admin operations.
 * Only Golden Macaroons should be allowed through
 */
@Priority(Priorities.AUTHENTICATION)
public class AdminAuthFilter extends AuthFilter<DPCAuthCredentials, OrganizationPrincipal> {

    private final MacaroonBakery bakery;

    @Inject
    public AdminAuthFilter(MacaroonBakery bakery, Authenticator<DPCAuthCredentials, OrganizationPrincipal> authenticator) {
        this.authenticator = authenticator;
        this.bakery = bakery;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final String macaroon = MacaroonHelpers.extractMacaroonFromRequest(requestContext, unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));

        // Validate Macaroon
        final List<Macaroon> m1;
        try {
            m1 = MacaroonBakery.deserializeMacaroon(macaroon);
        } catch (BakeryException e) {
            logger.error("Cannot deserialize Macaroon", e);
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }

        try {
            this.bakery.verifyMacaroon(m1);
        } catch (BakeryException e) {
            logger.error("Macaroon verification failed", e);
            throw new WebApplicationException(unauthorizedHandler.buildResponse(BEARER_PREFIX, realm));
        }

        // At this point, we should have exactly one Macaroon, anything else is a failure
        assert m1.size() == 1 : "Should only have a single Macaroon";

        // Ensure that we don't have any organization IDs
        // Since we ALWAYS generate organization_id caveats for tokens, its absence indicates that its a Golden Macaroon
        final boolean isGoldenMacaroon = MacaroonBakery.getCaveats(m1.get(0))
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
