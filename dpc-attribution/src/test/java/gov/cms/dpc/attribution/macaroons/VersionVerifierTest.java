package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;

public class VersionVerifierTest extends AbstractVerifierTest<VersionCaveatVerifier> {

    VersionVerifierTest() {
        super(new VersionCaveatVerifier(getTokenPolicy()));
    }
    @Override
    MacaroonCaveat getNonMatchingCaveat() {
        return new MacaroonCaveat(new MacaroonCondition("nothing", MacaroonCondition.Operator.EQ, "nothing"));
    }

    @Override
    MacaroonCaveat getWrongCaveat() {
        return new MacaroonCaveat(new MacaroonCondition("dpc_macaroon_version", MacaroonCondition.Operator.EQ, "0"));
    }

    @Override
    MacaroonCaveat getCorrectCaveat() {
        return new MacaroonCaveat(new MacaroonCondition("dpc_macaroon_version", MacaroonCondition.Operator.EQ, "1"));
    }

    @Override
    String provideFailureMessage() {
        return null;
    }
}
