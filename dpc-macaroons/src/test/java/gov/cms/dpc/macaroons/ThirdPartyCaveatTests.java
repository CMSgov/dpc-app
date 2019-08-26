package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.IThirdPartyKeyStore;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThirdPartyCaveatTests {

    @BeforeAll
    static void setup() {
//        final KeyPair firstPartyKey = BakeryKeyFactory.generateKeyPair();
//
//        // Add some third party keys
//        final KeyPair aliceKeys = BakeryKeyFactory.generateKeyPair();
//        final KeyPair bobKeys = BakeryKeyFactory.generateKeyPair();
//
//        thirdKeyStore.setPublicKey("http://alice", aliceKeys.getPublic());
//        thirdKeyStore.setPublicKey("http://bob", bobKeys.getPublic());


    }

    /**
     * TestMacaroonPaperFig6 implements an example flow as described in the macaroons paper:
     * http://theory.stanford.edu/~ataly/Papers/macaroons.pdf
     * There are three services, ts, fs, as:
     * ts is a store service which has deligated authority to a forum service fs.
     * The forum service wants to require its users to be logged into to an authentication service as.
     * <p>
     * The client obtains a macaroon from fs (minted by ts, with a third party caveat addressed to as).
     * The client obtains a discharge macaroon from as to satisfy this caveat.
     * The target service verifies the original macaroon it delegated to fs
     * No direct contact between as and ts is required
     */
    @Test
    void testThirdPartyRoundTrip() {
        final MemoryThirdPartyKeyStore thirdKeyStore = new MemoryThirdPartyKeyStore();
        final MacaroonBakery as = createBakery("as-loc", thirdKeyStore);
        final MacaroonBakery ts = createBakery("ts-loc", thirdKeyStore);
        final MacaroonBakery fs = createBakery("fs-loc", thirdKeyStore);

        // TS Creates a macaroon
        final Macaroon tsMacaroon = ts.createMacaroon(Collections.emptyList());

        // TS sends the Macaroon to fs which adds a third party caveat to be discharged
        final MacaroonCondition condition = new MacaroonCondition("user", MacaroonCondition.Operator.EQ, "bob");

        final Macaroon m1 = fs.addCaveats(tsMacaroon, new MacaroonCaveat("ac-loc", condition.toBytes()));
        assertEquals(1, ts.getCaveats(m1).size(), "Should have a single caveat");

        final List<Macaroon> discharged = ts.dischargeAll(Collections.singletonList(m1), (caveat, value) -> {
            assertEquals("as-loc", caveat.getLocation(), "Should have third-party caveats");
            return as.discharge(caveat, value);
        });
        assertTrue(discharged.size() > 1, "Should have more than 1 macaroon");
    }


    private static MacaroonBakery createBakery(String location, IThirdPartyKeyStore thirdPartyKeyStore) {

        final MemoryRootKeyStore keyStore = new MemoryRootKeyStore(new SecureRandom());

        return new MacaroonBakery.MacaroonBakeryBuilder(location, keyStore, thirdPartyKeyStore)
                .build();
    }
}
