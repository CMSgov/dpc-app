package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.attribution.config.TokenPolicy;
import gov.cms.dpc.macaroons.CaveatVerifier;
import gov.cms.dpc.macaroons.MacaroonCaveat;

import java.time.OffsetDateTime;
import java.util.Optional;

public class ExpirationCaveatVerifier implements CaveatVerifier {

    private final TokenPolicy.ExpirationPolicy expirationPolicy;

    ExpirationCaveatVerifier(TokenPolicy policy) {
        this.expirationPolicy = policy.getExpirationPolicy();
    }

    @Override
    public Optional<String> check(MacaroonCaveat caveat) {

        if (caveat.getKey().equals(ExpirationCaveatSupplier.EXPIRATION_KEY)) {

            final OffsetDateTime caveatExpiration = OffsetDateTime.parse(caveat.getValue());
            final OffsetDateTime expirationTime = OffsetDateTime.now().plus(expirationPolicy.getExpirationOffset(), expirationPolicy.getExpirationUnit());
            final boolean isBefore = caveatExpiration.isBefore(expirationTime);
            if (!isBefore) {
                return Optional.of("Caveat is expired");
            }
        }
        return Optional.empty();
    }
}
