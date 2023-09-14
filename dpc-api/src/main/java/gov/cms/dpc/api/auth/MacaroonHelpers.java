package gov.cms.dpc.api.auth;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.macaroons.CaveatSupplier;
import gov.cms.dpc.macaroons.MacaroonBakery;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.caveats.ExpirationCaveatSupplier;
import gov.cms.dpc.macaroons.caveats.VersionCaveatSupplier;
import org.apache.http.HttpHeaders;

import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MacaroonHelpers {

    private MacaroonHelpers() {
        // Not used
    }

    public static final String BEARER_PREFIX = "Bearer";
    public static final String ORGANIZATION_CAVEAT_KEY = "organization_id";
    static final String TOKEN_URI_PARAM = "token";

    /**
     * Extracts a Macaroon from the given {@link ContainerRequestContext}
     * First tries to get the value from the {@link HttpHeaders#AUTHORIZATION} header, if that fails (returns null) tries for the {@link MacaroonHelpers#TOKEN_URI_PARAM} query param.
     * Note: This does not validate that the Macaroon is actually valid, it simply pulls whatever {@link String} value it finds.
     *
     * @param requestContext       - {@link ContainerRequestContext} to extract Macaroon from
     * @param unauthorizedResponse - {@link Response} handler to use when extraction fails
     * @return - {@link String} Macaroon value from request
     */
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

    /**
     * Generate caveats for the given token Version
     *
     * @param tokenVersion   - {@link Integer} token version to generate caveats for
     * @param organizationID - {@link UUID} organization ID to restrict caveat to
     * @param tokenLifetime  - {@link Duration} lifetime of token
     * @return - {@link List} of {@link CaveatSupplier} to use for generating token
     */
    public static List<CaveatSupplier> generateCaveatsForToken(int tokenVersion, UUID organizationID, Duration tokenLifetime) {
        switch (tokenVersion) {
            case 1: {
                return generateV1Caveats(tokenLifetime, organizationID);
            }
            case 2: {
                return generateV2Caveats(tokenLifetime, organizationID);
            }
            default: {
                throw new IllegalArgumentException(String.format("Cannot created token with version: %s", tokenVersion));
            }
        }
    }

    public static Optional<UUID> extractOrgIDFromCaveats(List<Macaroon> macaroons) {
        final Macaroon rootMacaroon = macaroons.get(0);
            // Find the org_id caveat and extract the value
            return MacaroonBakery
                    .getCaveats(rootMacaroon)
                    .stream()
                    .map(MacaroonCaveat::getCondition)
                    .filter(condition -> condition.getKey().equals(ORGANIZATION_CAVEAT_KEY))
                    .map(condition -> UUID.fromString(condition.getValue()))
                    .findAny();
    }

    private static List<CaveatSupplier> generateV1Caveats(Duration tokenLifetime, UUID organizationID) {
        return generateDefaultCaveats(1, tokenLifetime, organizationID);
    }

    private static List<CaveatSupplier> generateV2Caveats(Duration tokenLifetime, UUID organizationID) {
        final List<CaveatSupplier> caveatSuppliers = new ArrayList<>(generateDefaultCaveats(2, tokenLifetime, organizationID));
        // The body of this local caveat is intentionally duplicated. We don't actually check it, we just need something in order to serialize correctly.
        // In the future, we might add specific caveats that the discharger can check before discharging.
        caveatSuppliers.add(() -> new MacaroonCaveat("local", new MacaroonCondition(ORGANIZATION_CAVEAT_KEY, MacaroonCondition.Operator.EQ, organizationID.toString())));
        return caveatSuppliers;
    }

    private static List<CaveatSupplier> generateDefaultCaveats(int tokenVersion, Duration tokenLifetime, UUID organizationID) {
        return List.of(
                new VersionCaveatSupplier(tokenVersion),
                new ExpirationCaveatSupplier(tokenLifetime),
                () -> new MacaroonCaveat("", new MacaroonCondition(ORGANIZATION_CAVEAT_KEY, MacaroonCondition.Operator.EQ, organizationID.toString())));
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
