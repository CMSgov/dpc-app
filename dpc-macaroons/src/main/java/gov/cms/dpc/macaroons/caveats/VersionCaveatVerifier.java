package gov.cms.dpc.macaroons.caveats;

import gov.cms.dpc.macaroons.config.TokenPolicy;
import gov.cms.dpc.macaroons.CaveatVerifier;
import gov.cms.dpc.macaroons.MacaroonCondition;

import java.util.Optional;

import static gov.cms.dpc.macaroons.caveats.VerifierConstants.NO_MATCH;

/**
 * Implementation of {@link CaveatVerifier} which verifies that token version meets a minimum threshold
 */
public class VersionCaveatVerifier implements CaveatVerifier {

    private final int minimumVersion;

    public VersionCaveatVerifier(TokenPolicy policy) {
        this.minimumVersion = policy.getVersionPolicy().getMinimumVersion();
    }

    @Override
    public Optional<String> check(MacaroonCondition caveat) {

        if (caveat.getKey().equals(VersionCaveatSupplier.VERSION_KEY)) {
            final int tokenVersion = Integer.parseInt(caveat.getValue());
            if (tokenVersion < minimumVersion) {
                return Optional.of(String.format("Token version '%d' is not supported. Minimum is: %s", tokenVersion, minimumVersion));
            }
            return Optional.empty();
        }
        return Optional.of(NO_MATCH);
    }
}
