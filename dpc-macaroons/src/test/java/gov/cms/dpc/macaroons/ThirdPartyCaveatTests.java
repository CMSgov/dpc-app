package gov.cms.dpc.macaroons;

import com.codahale.xsalsa20poly1305.SecretBox;
import org.junit.jupiter.api.Test;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import java.security.SecureRandom;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ThirdPartyCaveatTests {

    @Test
    void testSecretPartRoundTrip() {
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
}
