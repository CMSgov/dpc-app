package gov.cms.dpc.macaroons.caveats;

import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.CaveatVerifier;
import gov.cms.dpc.macaroons.MacaroonCondition;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Implementation of {@link CaveatVerifier} which verifies that the token is not expired
 * Expiration is determined based on the provided {@link TokenPolicy.ExpirationPolicy}
 */
public class ExpirationCaveatVerifier implements CaveatVerifier {

    public static final String CAVEAT_INVALID = "Caveat is expired";

    public ExpirationCaveatVerifier(TokenPolicy policy) { }

    @Override
    public Optional<String> check(MacaroonCondition caveat) {

        if (caveat.getKey().equals(ExpirationCaveatSupplier.EXPIRATION_KEY)) {

            final OffsetDateTime caveatExpiration = OffsetDateTime.parse(caveat.getValue());
            final OffsetDateTime currentTime = OffsetDateTime.now(ZoneOffset.UTC);
            final boolean isBefore = caveatExpiration.isBefore(currentTime);
            if (isBefore) {
                return Optional.of(CAVEAT_INVALID);
            }
            return Optional.empty();
        }
        return Optional.of(VerifierConstants.NO_MATCH);
    }
}
