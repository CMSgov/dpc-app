package gov.cms.dpc.macaroons;

import com.codahale.xsalsa20poly1305.Keys;
import com.codahale.xsalsa20poly1305.SecretBox;
import gov.cms.dpc.macaroons.helpers.BakeryKeyFactory;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.junit.jupiter.api.Test;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ConditionParsingTests {

    @Test
    void testSimpleCaveatParsing() {
        final MacaroonCondition macaroonCondition = MacaroonCondition.parseFromString("test = valid");
        assertAll(() -> assertEquals("test", macaroonCondition.getKey(), "Key should match"),
                () -> assertEquals(MacaroonCondition.Operator.EQ, macaroonCondition.getOp(), "Op should match"),
                () -> assertEquals("valid", macaroonCondition.getValue(), "Value should match"));
    }

    @Test
    void testPoorlyFormattedCaveatParsing() {
        // Test spaces
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test =valid"), "Caveats need spaces between entities"),
                () -> assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test=valid"), "Caveats need spaces between entities"),
                () -> assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test= valid"), "Caveats need spaces between entities"));
    }

    @Test
    void testInvalidOperationValueParsing() {
        assertAll(() -> assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test ~ valid"), "Should throw for invalid operation type"));
    }

    @Test
    void testMalformedCaveatParsing() {
        assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test ="), "Should not parse malformed caveat");
        assertThrows(IllegalArgumentException.class, () -> MacaroonCondition.parseFromString("test id = hello"), "Should not parse caveat with strings in key");
    }

    @Test
    void testSecretEncodingRoundTrip() {
        // Create a test key pairs for first party and third party
        final Curve25519 instance = Curve25519.getInstance(Curve25519.BEST);

        final Curve25519KeyPair thirdParty = instance.generateKeyPair();
        final Curve25519KeyPair firstParty = instance.generateKeyPair();

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
    void testSecretEncodingCustomKeys() {
        final Curve25519KeyPair firstParty = BakeryKeyFactory.generateKeyPair();
        final Curve25519KeyPair thirdParty = BakeryKeyFactory.generateKeyPair();

        final String testMessage = "This is a test message";
        final String testKey = "this is a test key";

        final SecureRandom random = new SecureRandom();

        final byte[] testNonce = new byte[24];
        random.nextBytes(testNonce);

        // Encrypt it with the first party priv key and the third party pub key
        final byte[] thirdPartyPrivate = BakeryKeyFactory.unwrapPrivateKeyBytes(thirdParty);
        final byte[] thirdPartyPublic = Keys.generatePublicKey(thirdPartyPrivate);

        final byte[] firstPartyPrivate = BakeryKeyFactory.unwrapPrivateKeyBytes(firstParty);
        final byte[] firstPartyPublic = Keys.generatePublicKey(firstPartyPrivate);

        // encrypt the message
        final byte[] sealed = MacaroonBakery.encodeSecretPart(thirdPartyPublic, firstPartyPrivate, testNonce, testKey, testMessage);

        final SecretBox box = new SecretBox(firstPartyPublic, thirdPartyPrivate);
        final byte[] openedBytes = box.open(testNonce, sealed).orElseThrow(() -> new RuntimeException("Cannot open box"));
        assertTrue(new String(openedBytes).endsWith(testMessage), "Should have the same message at the end");


    }
}
