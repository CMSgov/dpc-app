package gov.cms.dpc.macaroons.caveats;

import gov.cms.dpc.macaroons.MacaroonCaveat;
import gov.cms.dpc.macaroons.MacaroonCondition;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class CaveatSupplierTests {

    @Test
    void testExpirationSupplier() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        final Duration lifetime = Duration.of(5, ChronoUnit.MINUTES);
        final MacaroonCaveat caveat = new ExpirationCaveatSupplier(lifetime).get();

        assertAll(() -> assertNotNull(caveat),
                () -> assertEquals(ExpirationCaveatSupplier.EXPIRATION_KEY, caveat.getCondition().getKey(), "Should have correct key"),
                () -> assertEquals(MacaroonCondition.Operator.EQ, caveat.getCondition().getOp(), "Should have equals op"),
                () -> assertTrue(OffsetDateTime.parse(caveat.getCondition().getValue()).truncatedTo(ChronoUnit.MINUTES)
                        .isEqual(now.plus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MINUTES)), "Should be 5 minutes in the future"));
    }

    @Test
    void testVersionSupplier() {
        final MacaroonCaveat caveat = new VersionCaveatSupplier(1).get();

        assertAll(() -> assertNotNull(caveat),
                () -> assertEquals(VersionCaveatSupplier.VERSION_KEY, caveat.getCondition().getKey(), "Should have correct key"),
                () -> assertEquals(MacaroonCondition.Operator.EQ, caveat.getCondition().getOp(), "Should have equals op"),
                () -> assertEquals("1", caveat.getCondition().getValue(), "Should have correct version"));
    }
}
