package gov.cms.dpc.macaroons.verifiers;

import gov.cms.dpc.macaroons.CaveatVerifier;
import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.macaroons.caveats.VerifierConstants;
import gov.cms.dpc.macaroons.config.TokenPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(BufferedLoggerHandler.class)
@SuppressWarnings("OptionalGetWithoutIsPresent")
@DisplayName("Caveat matching")

abstract class AbstractVerifierTest<V extends CaveatVerifier> {

    private final V verifier;

    AbstractVerifierTest(V verifier) {
        this.verifier = verifier;
    }

    @Test
@DisplayName("Verify non-matching macaroon caveat 🥳")
    final void testNonMatchingCaveat() {
        final MacaroonCaveat caveat = getNonMatchingCaveat();
        final Optional<String> response = this.verifier.check(caveat.getCondition());
        assertAll(() -> assertTrue(response.isPresent(), "Should have failure message"),
                () -> assertEquals(VerifierConstants.NO_MATCH, response.get(), "Should have not found caveat"));
    }

    @Test
@DisplayName("Verify wrong macaroon caveat 🥳")
    final void testWrongCaveat() {
        final MacaroonCaveat caveat = getWrongCaveat();
        final Optional<String> response = this.verifier.check(caveat.getCondition());
        assertTrue(response.isPresent(), "Should have failure message");
    }

    @Test
@DisplayName("Get correct macaroon caveat 🥳")
    final void testCorrectCaveat() {
        final MacaroonCaveat caveat = getCorrectCaveat();
        final Optional<String> response = this.verifier.check(caveat.getCondition());
        assertTrue(response.isEmpty(), "Should not have failure message");
    }

    abstract MacaroonCaveat getNonMatchingCaveat();

    abstract MacaroonCaveat getWrongCaveat();

    abstract MacaroonCaveat getCorrectCaveat();

    abstract String provideFailureMessage();

    static TokenPolicy getTokenPolicy() {
        final TokenPolicy.ExpirationPolicy ep = new TokenPolicy.ExpirationPolicy();
        ep.setExpirationUnit(ChronoUnit.YEARS);
        ep.setExpirationOffset(1);

        final TokenPolicy.VersionPolicy vp = new TokenPolicy.VersionPolicy();
        vp.setCurrentVersion(1);
        vp.setMinimumVersion(1);

        final TokenPolicy tokenPolicy = new TokenPolicy();
        tokenPolicy.setVersionPolicy(vp);
        tokenPolicy.setExpirationPolicy(ep);
        return tokenPolicy;
    }
}
