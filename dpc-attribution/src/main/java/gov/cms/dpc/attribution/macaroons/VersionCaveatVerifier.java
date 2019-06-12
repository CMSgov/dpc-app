package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.macaroons.CaveatVerifier;
import gov.cms.dpc.macaroons.MacaroonCaveat;

import java.util.Optional;

public class VersionCaveatVerifier implements CaveatVerifier {

    VersionCaveatVerifier() {
        // Not used
    }

    @Override
    public Optional<String> check(MacaroonCaveat caveat) {

        if (caveat.getKey().equals(VersionCaveatSupplier.VERSION_KEY)) {
            final boolean isEqual = caveat.getValue().equals("1");
            if (!isEqual) {
                return Optional.of(String.format("Expected version %s. Got %s", "1", caveat.getValue()));
            }
        }
        return Optional.empty();
    }
}
