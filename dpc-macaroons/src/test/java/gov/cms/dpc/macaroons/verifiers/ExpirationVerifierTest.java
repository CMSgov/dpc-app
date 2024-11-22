package gov.cms.dpc.macaroons.verifiers;

import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import gov.cms.dpc.macaroons.caveats.ExpirationCaveatVerifier;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

class ExpirationVerifierTest extends AbstractVerifierTest<ExpirationCaveatVerifier> {

    ExpirationVerifierTest() {
        super(new ExpirationCaveatVerifier(getTokenPolicy()));
    }

    @Override
    MacaroonCaveat getNonMatchingCaveat() {
        return new MacaroonCaveat(new MacaroonCondition("nothing", MacaroonCondition.Operator.EQ, "nothing"));
    }

    @Override
    MacaroonCaveat getWrongCaveat() {
        return new MacaroonCaveat(new MacaroonCondition("expires", MacaroonCondition.Operator.EQ, LocalDate.of(1990, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC).toString()));
    }

    @Override
    MacaroonCaveat getCorrectCaveat() {
        return new MacaroonCaveat(new MacaroonCondition("expires", MacaroonCondition.Operator.EQ, OffsetDateTime.now(ZoneOffset.UTC).plus(2, ChronoUnit.YEARS).toString()));
    }

    @Override
    String provideFailureMessage() {
        return ExpirationCaveatVerifier.CAVEAT_INVALID;
    }
}
