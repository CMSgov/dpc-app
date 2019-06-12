package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.macaroons.CaveatVerifier;
import gov.cms.dpc.macaroons.MacaroonCaveat;

import java.time.OffsetDateTime;
import java.util.Optional;

public class ExpirationCaveatVerifier implements CaveatVerifier {

    ExpirationCaveatVerifier() {
        // Not used
    }

    @Override
    public Optional<String> check(MacaroonCaveat caveat) {

        if (caveat.getKey().equals(ExpirationCaveatSupplier.EXPIRATION_KEY)) {
            final boolean isBefore = OffsetDateTime.now().isBefore(OffsetDateTime.parse(caveat.getValue()));
            if (!isBefore) {
                return Optional.of("Caveat is expired");
            }
        }

        return Optional.empty();
    }
}
