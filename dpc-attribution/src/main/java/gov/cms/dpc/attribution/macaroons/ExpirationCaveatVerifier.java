package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.attribution.config.TokenPolicy;
import gov.cms.dpc.macaroons.CaveatVerifier;
import gov.cms.dpc.macaroons.MacaroonCaveat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Implementation of {@link CaveatVerifier} which verifies that the token is not expired
 * Expiration is determined based on the provided {@link TokenPolicy.ExpirationPolicy}
 */
public class ExpirationCaveatVerifier implements CaveatVerifier {

    static final String CAVEAT_INVALID = "Caveat is expired";
    private final TokenPolicy.ExpirationPolicy expirationPolicy;

    ExpirationCaveatVerifier(TokenPolicy policy) {
        this.expirationPolicy = policy.getExpirationPolicy();
    }

    @Override
    public Optional<String> check(MacaroonCaveat caveat) {

        if (caveat.getKey().equals(ExpirationCaveatSupplier.EXPIRATION_KEY)) {

            final OffsetDateTime caveatExpiration = OffsetDateTime.parse(caveat.getValue());
            final OffsetDateTime expirationTime = OffsetDateTime.now(ZoneOffset.UTC).plus(expirationPolicy.getExpirationOffset(), expirationPolicy.getExpirationUnit());
            final boolean isBefore = caveatExpiration.isBefore(expirationTime);
            if (!isBefore) {
                return Optional.of(CAVEAT_INVALID);
            }
            return Optional.empty();
        }
        return Optional.of(VerifierConstants.NO_MATCH);
    }
}
