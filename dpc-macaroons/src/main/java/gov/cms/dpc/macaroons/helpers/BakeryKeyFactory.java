package gov.cms.dpc.macaroons.helpers;

import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

/**
 * Helper factory for generating ECC KeyPairs
 */
public class BakeryKeyFactory {

    public static Curve25519KeyPair generateKeyPair() {
        return Curve25519.getInstance(Curve25519.BEST).generateKeyPair();
    }
}
