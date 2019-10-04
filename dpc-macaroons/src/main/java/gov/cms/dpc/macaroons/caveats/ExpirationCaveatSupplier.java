package gov.cms.dpc.macaroons.caveats;

import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.CaveatSupplier;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Implementation of {@link CaveatSupplier} which generates an expiration caveat based on the provided {@link TokenPolicy.ExpirationPolicy}
 */
public class ExpirationCaveatSupplier implements CaveatSupplier {

    static final String EXPIRATION_KEY = "expires";

    private final TokenPolicy.ExpirationPolicy expirationPolicy;

    public ExpirationCaveatSupplier(TokenPolicy policy) {
        this.expirationPolicy = policy.getExpirationPolicy();
    }

    @Override
    public MacaroonCaveat get() {
        final OffsetDateTime expiryTime = OffsetDateTime.now(ZoneOffset.UTC)
                .plus(expirationPolicy.getExpirationOffset(), expirationPolicy.getExpirationUnit());
        return new MacaroonCaveat(new MacaroonCondition(EXPIRATION_KEY, MacaroonCondition.Operator.EQ, expiryTime.toString()));
    }
}
