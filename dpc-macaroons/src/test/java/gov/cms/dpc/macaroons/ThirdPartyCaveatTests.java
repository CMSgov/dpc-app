package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.IThirdPartyKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class ThirdPartyCaveatTests {

    private static MacaroonBakery as;
    private static MacaroonBakery ts;
    private static MacaroonBakery fs;

    @BeforeAll
    static void setup() {
        MemoryThirdPartyKeyStore thirdKeyStore = new MemoryThirdPartyKeyStore();
        as = createBakery("as-loc", thirdKeyStore);
        ts = createBakery("ts-loc", thirdKeyStore);
        fs = createBakery("fs-loc", thirdKeyStore);
    }

    /**
     * TestMacaroonPaperFig6 implements an example flow as described in the macaroons paper:
     * http://theory.stanford.edu/~ataly/Papers/macaroons.pdf
     * There are three services, ts, fs, as:
     * ts is a store service which has delegated authority to a forum service fs.
     * The forum service wants to require its users to be logged into to an authentication service as.
     * <p>
     * The client obtains a macaroon from fs (minted by ts, with a third party caveat addressed to as).
     * The client obtains a discharge macaroon from as to satisfy this caveat.
     * The target service verifies the original macaroon it delegated to fs
     * No direct contact between as and ts is required
     */
    @Test
    void testThirdPartyRoundTrip() {
        // TS Creates a macaroon
        final Macaroon tsMacaroon = ts.createMacaroon(Collections.emptyList());

        // TS sends the Macaroon to fs which adds a third party caveat to be discharged
        final MacaroonCondition condition = new MacaroonCondition("user", MacaroonCondition.Operator.EQ, "bob");

        final Macaroon m1 = fs.addCaveats(tsMacaroon, new MacaroonCaveat("as-loc", condition.toBytes()));
        assertEquals(1, MacaroonBakery.getCaveats(m1).size(), "Should have a single caveat");

        final List<Macaroon> discharged = ts.dischargeAll(Collections.singletonList(m1), (caveat, value) -> {
            assertEquals("as-loc", caveat.getLocation(), "Should have third-party caveats");
            return as.discharge(caveat, value);
        });
        assertTrue(discharged.size() > 1, "Should have more than 1 macaroon");

        ts.verifyMacaroon(discharged);
    }

    @Test
    void testThirdPartyFailsWithoutDischarge() {
        // TS Creates a macaroon
        final Macaroon tsMacaroon = ts.createMacaroon(Collections.emptyList());

        // TS sends the Macaroon to fs which adds a third party caveat to be discharged
        final MacaroonCondition condition = new MacaroonCondition("user", MacaroonCondition.Operator.EQ, "bob");

        final List<Macaroon> macaroons = Collections.singletonList(
                fs.addCaveats(tsMacaroon, new MacaroonCaveat("as-loc", condition.toBytes()))
        );

        final BakeryException bakeryException = assertThrows(BakeryException.class, () -> ts.verifyMacaroon(macaroons));
        assertEquals("Couldn't verify 3rd party macaroon, because no discharged macaroon was provided to the verifier.", bakeryException.getMessage(), "Should have correct error message");
    }


    private static MacaroonBakery createBakery(String location, IThirdPartyKeyStore thirdPartyKeyStore) {

        final MemoryRootKeyStore keyStore = new MemoryRootKeyStore(new SecureRandom());

        return new MacaroonBakery.MacaroonBakeryBuilder(location, keyStore, thirdPartyKeyStore)
                .build();
    }
}
