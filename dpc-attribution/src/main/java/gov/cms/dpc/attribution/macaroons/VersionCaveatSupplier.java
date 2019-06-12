package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.macaroons.CaveatSupplier;
import gov.cms.dpc.macaroons.MacaroonCaveat;

public class VersionCaveatSupplier implements CaveatSupplier {

    static final String VERSION_KEY = "dpc_macaroon_version";

    VersionCaveatSupplier() {

    }

    public MacaroonCaveat get() {
        return new MacaroonCaveat(VERSION_KEY, MacaroonCaveat.Operator.EQ, "1");
    }
}
