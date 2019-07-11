package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.macaroons.MacaroonCaveat;

public class VersionVerifierTest extends AbstractVerifierTest<VersionCaveatVerifier> {

    VersionVerifierTest() {
        super(new VersionCaveatVerifier(getTokenPolicy()));
    }
    @Override
    MacaroonCaveat getNonMatchingCaveat() {
        return new MacaroonCaveat("nothing", MacaroonCaveat.Operator.EQ, "nothing");
    }

    @Override
    MacaroonCaveat getWrongCaveat() {
        return new MacaroonCaveat("dpc_macaroon_version", MacaroonCaveat.Operator.EQ, "0");
    }

    @Override
    MacaroonCaveat getCorrectCaveat() {
        return new MacaroonCaveat("dpc_macaroon_version", MacaroonCaveat.Operator.EQ, "1");
    }

    @Override
    String provideFailureMessage() {
        return null;
    }
}
