package gov.cms.dpc.api.auth.macaroons;

import gov.cms.dpc.api.config.TokenPolicy;
import gov.cms.dpc.macaroons.CaveatSupplier;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;

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
        return new MacaroonCaveat(new MacaroonCondition(VERSION_KEY, MacaroonCondition.Operator.EQ, this.tokenVersion));
    }
}
