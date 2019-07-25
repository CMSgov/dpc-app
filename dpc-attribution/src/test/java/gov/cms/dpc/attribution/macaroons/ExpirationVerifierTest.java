package gov.cms.dpc.attribution.macaroons;

import gov.cms.dpc.macaroons.MacaroonCaveat;
import org.junit.jupiter.api.Disabled;

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
        return new MacaroonCaveat("nothing", MacaroonCaveat.Operator.EQ, "nothing");
    }

    @Override
    MacaroonCaveat getWrongCaveat() {
        return new MacaroonCaveat("expires", MacaroonCaveat.Operator.EQ, LocalDate.of(1990, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC).toString());
    }

    @Override
    MacaroonCaveat getCorrectCaveat() {
        return new MacaroonCaveat("expires", MacaroonCaveat.Operator.EQ, OffsetDateTime.now(ZoneOffset.UTC).plus(2, ChronoUnit.YEARS).toString());
    }

    @Override
    String provideFailureMessage() {
        return ExpirationCaveatVerifier.CAVEAT_INVALID;
    }
}
