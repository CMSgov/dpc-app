package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.attribution.config.TokenPolicy;
import gov.cms.dpc.macaroons.CaveatSupplier;
import gov.cms.dpc.macaroons.MacaroonCaveat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Implementation of {@link CaveatSupplier} which generates an expiration caveat based on the provided {@link ExpirationPolicy}
 */
public class ExpirationCaveatSupplier implements CaveatSupplier {

    static final String EXPIRATION_KEY = "expires";

    private final TokenPolicy.ExpirationPolicy expirationPolicy;

    ExpirationCaveatSupplier(TokenPolicy policy) {
        this.expirationPolicy = policy.getExpirationPolicy();
    }

    @Override
    public MacaroonCaveat get() {
        final OffsetDateTime expiryTime = OffsetDateTime.now(ZoneOffset.UTC)
                .plus(expirationPolicy.getExpirationOffset(), expirationPolicy.getExpirationUnit());
        return new MacaroonCaveat(EXPIRATION_KEY, MacaroonCaveat.Operator.EQ, expiryTime.toString());
    }
}
