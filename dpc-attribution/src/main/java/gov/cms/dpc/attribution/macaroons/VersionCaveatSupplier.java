package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.attribution.config.TokenPolicy;
import gov.cms.dpc.macaroons.CaveatSupplier;
import gov.cms.dpc.macaroons.MacaroonCaveat;

/**
 * Implementation of {@link CaveatSupplier} which generates a version token based on the provided {@link TokenPolicy}
 */
public class VersionCaveatSupplier implements CaveatSupplier {

    static final String VERSION_KEY = "dpc_macaroon_version";

    private final String tokenVersion;

    VersionCaveatSupplier(TokenPolicy policy) {
        this.tokenVersion = Integer.toString(policy.getVersionPolicy().getCurrentVersion());
    }

    @Override
    public MacaroonCaveat get() {
        return new MacaroonCaveat(VERSION_KEY, MacaroonCaveat.Operator.EQ, this.tokenVersion);
    }
}
