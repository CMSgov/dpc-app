package gov.cms.dpc.macaroons;

import com.codahale.xsalsa20poly1305.SecretBox;
import gov.cms.dpc.macaroons.thirdparty.BakeryKeyPair;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.whispersystems.curve25519.Curve25519;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Caveat condition parsing")
class ConditionParsingTests {

    @Test
    @DisplayName("Parse simple caveat 🥳")
    void testSimpleCaveatParsing() {
        final MacaroonCondition macaroonCondition = MacaroonCondition.parseFromString("test = valid");
        assertAll(() -> assertEquals("test", macaroonCondition.getKey(), "Key should match"),
                () -> assertEquals(MacaroonCondition.Operator.EQ, macaroonCondition.getOp(), "Op should match"),
                () -> assertEquals("valid", macaroonCondition.getValue(), "Value should match"));
    }

    @Test
    @DisplayName("Parse poorly-formatted caveat 🤮")
    void testPoorlyFormattedCaveatParsing() {
        // Test spaces
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test =valid"), "Caveats need spaces between entities"),
                () -> assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test=valid"), "Caveats need spaces between entities"),
                () -> assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test= valid"), "Caveats need spaces between entities"));
    }

    @Test
    @DisplayName("Parse invalid operation value 🤮")
    void testInvalidOperationValueParsing() {
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test ~ valid"), "Should throw for invalid operation type"));
    }

    @Test
    @DisplayName("Parse malformed caveat 🤮")
    void testMalformedCaveatParsing() {
        assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test ="), "Should not parse malformed caveat");
        assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test id = hello"), "Should not parse caveat with strings in key");
    }

    @Test
    @DisplayName("Secret encoding round trip 🥳")
    void testSecretEncodingRoundTrip() {
        // Create a test key pairs for first party and third party
        final Curve25519 instance = Curve25519.getInstance(Curve25519.BEST);

        final BakeryKeyPair thirdParty = BakeryKeyPair.generate();
        final BakeryKeyPair firstParty = BakeryKeyPair.generate();

        final String testMessage = "This is a test message";
        final String testKey = "this is a test key";

        final SecureRandom random = new SecureRandom();

        final byte[] testNonce = new byte[24];
        random.nextBytes(testNonce);
        final byte[] sealed = MacaroonBakery.encodeSecretPart(thirdParty.getPublicKey(), firstParty.getPrivateKey(), testNonce, testKey, testMessage);

        final SecretBox box = new SecretBox(firstParty.getPublicKey(), thirdParty.getPrivateKey());
        final byte[] openedBytes = box.open(testNonce, sealed).orElseThrow(() -> new RuntimeException("Cannot open box"));
        assertTrue(new String(openedBytes).endsWith(testMessage), "Should have the same message at the end");
    }

    @Test
    @DisplayName("Secret encoding with custom keys 🥳")
    void testSecretEncodingCustomKeys() {
        final BakeryKeyPair firstParty = BakeryKeyPair.generate();
        final BakeryKeyPair thirdParty = BakeryKeyPair.generate();

        final String testMessage = "This is a test message";
        final String testKey = "this is a test key";

        final SecureRandom random = new SecureRandom();

        final byte[] testNonce = new byte[24];
        random.nextBytes(testNonce);

        // Encrypt it with the first party priv key and the third party pub key
        final byte[] thirdPartyPrivate = thirdParty.getPrivateKey();
        final byte[] thirdPartyPublic = thirdParty.getPublicKey();

        final byte[] firstPartyPrivate = firstParty.getPrivateKey();
        final byte[] firstPartyPublic = firstParty.getPublicKey();

        // encrypt the message
        final byte[] sealed = MacaroonBakery.encodeSecretPart(thirdPartyPublic, firstPartyPrivate, testNonce, testKey, testMessage);

        final SecretBox box = new SecretBox(firstPartyPublic, thirdPartyPrivate);
        final byte[] openedBytes = box.open(testNonce, sealed).orElseThrow(() -> new RuntimeException("Cannot open box"));
        assertTrue(new String(openedBytes).endsWith(testMessage), "Should have the same message at the end");


    }
}
