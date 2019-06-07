package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BakeryTest {

    private static MacaroonsBakery bakery;

    @BeforeAll
    static void setup() {
        bakery = new MacaroonsBakery("http://localhost", new MemoryRootKeyStore(new SecureRandom()));
    }

    @Test
    void testSerializationDeserialization() {
        final Macaroon testMacaroon = bakery
                .createMacaroon(Collections.singletonList(
                        new MacaroonCaveat("test_id",
                                MacaroonCaveat.Operator.EQ, "1234")));

        final byte[] macaroonBytes = bakery.serializeMacaroon(testMacaroon);
        final Macaroon mac2 = bakery.deserializeMacaroon(new String(macaroonBytes));
        assertEquals(testMacaroon, mac2, "Macaroons should be equal");
    }

    @Test
    void testCaveatParsing() {
        final List<MacaroonCaveat> caveatList = Collections.singletonList(
                new MacaroonCaveat("test_id",
                        MacaroonCaveat.Operator.EQ, "1234"));
        final Macaroon testMacaroon = bakery
                .createMacaroon(caveatList);

        assertArrayEquals(caveatList.toArray(), bakery.getCaveats(testMacaroon).toArray(), "Should have equal caveats");
    }

    @Test
    void testThirdPartyCaveat() {
        assertThrows(UnsupportedOperationException.class, () ->
                bakery
                        .createMacaroon(Collections.singletonList(
                                new MacaroonCaveat("http://test.local",
                                        "test_third_id", MacaroonCaveat.Operator.NEQ,
                                        "wrong value"))));
    }
}
