package gov.cms.dpc.macaroons;

import com.github.nitram509.jmacaroons.Macaroon;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import gov.cms.dpc.macaroons.store.MemoryRootKeyStore;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
import gov.cms.dpc.macaroons.thirdparty.MemoryThirdPartyKeyStore;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class BakeryTest {

    private static final MemoryThirdPartyKeyStore thirdParty = new MemoryThirdPartyKeyStore();
    private static final BakeryKeyPair bakeryKey = BakeryKeyPair.generate();
    private static MacaroonBakery bakery;

    @BeforeAll
    static void setup() {
        bakery = new MacaroonBakery("http://localhost",
                new MemoryRootKeyStore(new SecureRandom()),
                thirdParty,
                bakeryKey,
                Collections.emptyList(),
                Collections.emptyList());
    }

    @Test
    void testSerializationDeserialization() {
        macaroonSerializationTest(false);
    }

    @Test
    void testBase64EncodeDecode() {
        macaroonSerializationTest(true);
    }

    @Test
    void testCaveatParsing() {
        final List<MacaroonCaveat> caveatList = Collections.singletonList(
                new MacaroonCaveat("", new MacaroonCondition("test_id",
                        MacaroonCondition.Operator.EQ, "1234")));
        final Macaroon testMacaroon = bakery
                .createMacaroon(caveatList);

        assertArrayEquals(caveatList.toArray(), MacaroonBakery.getCaveats(testMacaroon).toArray(), "Should have equal caveats");
    }

    @Test
    void testLocalThirdPartyCaveat() {
        List<MacaroonCaveat> caveats = new ArrayList<>();
        caveats.add(new MacaroonCaveat("", new MacaroonCondition("account", MacaroonCondition.Operator.EQ, "3735928559")));
        caveats.add(new MacaroonCaveat("http://localhost",
                new MacaroonCondition(
                        "user", MacaroonCondition.Operator.EQ,
                        "Alice")));
        final Macaroon macaroon = bakery
                .createMacaroon(caveats);

        // Discharge it
        final List<Macaroon> macaroons = bakery.dischargeAll(Collections.singletonList(macaroon), (caveat, value) -> {
            assertEquals("http://localhost", caveat.getLocation(), "Should have local caveat");
            return bakery.discharge(caveat, value);
        });

        bakery.verifyMacaroon(macaroons, "account = 3735928559");
    }

    private static void macaroonSerializationTest(boolean base64) {
        final Macaroon testMacaroon = bakery
                .createMacaroon(Collections.singletonList(
                        new MacaroonCaveat("", new MacaroonCondition("test_id",
                                MacaroonCondition.Operator.EQ, "1234"))));

        final byte[] macaroonBytes = bakery.serializeMacaroon(testMacaroon, base64);
        final List<Macaroon> macaroons = MacaroonBakery.deserializeMacaroon(new String(macaroonBytes, StandardCharsets.UTF_8));
        final Macaroon mac2 = macaroons.get(0);
        assertAll(() -> assertEquals(1, macaroons.size(), "Should only have a single macaroon"),
                () -> assertEquals(testMacaroon, mac2, "Macaroons should be equal"));
    }

    @Test
    void testDefaultCaveatChecking() {

        final CaveatVerifier verifier = caveat -> {
            if (caveat.toString().equals("test_id = 1234")) {
                return Optional.empty();
            }
            return Optional.of("Caveat is not satisfied");
        };
        final MacaroonBakery caveatBakery = new MacaroonBakery.MacaroonBakeryBuilder("http://test.local", new MemoryRootKeyStore(new SecureRandom()), thirdParty)
                .addDefaultVerifier(verifier)
                .build();

        final Macaroon macaroon = caveatBakery
                .createMacaroon(Collections.singletonList(
                        new MacaroonCaveat("", new MacaroonCondition("test_id",
                                MacaroonCondition.Operator.EQ, "1234"))));

        caveatBakery.verifyMacaroon(Collections.singletonList(macaroon));

        // Add a caveat and try to validate again, which should fail
        final Macaroon macaroon1 = caveatBakery.addCaveats(macaroon, new MacaroonCaveat("", new MacaroonCondition("expires", MacaroonCondition.Operator.LT, "now")));

        assertThrows(BakeryException.class, () -> caveatBakery.verifyMacaroon(Collections.singletonList(macaroon1)));

        // Add a verifier and try again
        caveatBakery.verifyMacaroon(Collections.singletonList(macaroon1), "expires < now");

        // Add an incorrect verifier, which should fail
        assertThrows(BakeryException.class, () -> caveatBakery.verifyMacaroon(Collections.singletonList(macaroon1), "expires < wrong"), "Verification should fail");
    }

    @Test
    void testDefaultCaveatSuppliers() {

        final MacaroonCaveat test_caveat = new MacaroonCaveat("", new MacaroonCondition("test_caveat", MacaroonCondition.Operator.EQ, "1"));
        final CaveatSupplier testSupplier = () -> test_caveat;
        final CaveatVerifier testVerifier = (caveat) -> {
            if (caveat.getKey().equals("test_caveat")) {
                assertEquals(caveat, test_caveat.getCondition(), "Caveats should match");
            }
            return Optional.empty();
        };

        final MacaroonBakery caveatBakery = new MacaroonBakery.MacaroonBakeryBuilder("http://test.local", new MemoryRootKeyStore(new SecureRandom()), thirdParty)
                .addDefaultCaveatSupplier(testSupplier)
                .addDefaultVerifier(testVerifier)
                .build();

        final Macaroon macaroon = caveatBakery
                .createMacaroon(Collections.singletonList(
                        new MacaroonCaveat("", new MacaroonCondition("test_id",
                                MacaroonCondition.Operator.EQ, "1234"))));

        final List<MacaroonCaveat> macCaveats = MacaroonBakery.getCaveats(macaroon);
        assertEquals(2, macCaveats.size(), "Should have two caveats");

        caveatBakery.verifyMacaroon(Collections.singletonList(macaroon), "test_id = 1234");
    }
}
