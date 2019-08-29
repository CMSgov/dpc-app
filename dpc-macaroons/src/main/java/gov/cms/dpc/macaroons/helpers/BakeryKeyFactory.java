package gov.cms.dpc.macaroons.helpers;

import com.codahale.xsalsa20poly1305.Keys;
import com.github.nitram509.jmacaroons.MacaroonsConstants;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

import java.security.*;
import java.security.interfaces.ECPrivateKey;

/**
 * Helper factory for generating ECC KeyPairs
 */
public class BakeryKeyFactory {

    private static final KeyPairGenerator generator = initialize();

    public static Curve25519KeyPair generateKeyPair() {
        return Curve25519.getInstance(Curve25519.BEST).generateKeyPair();
    }

    /**
     * Unwrap the raw key bytes from the underlying {@link java.security.interfaces.ECPrivateKey}.
     * We need this because BouncyCastle encodes a lot of additional information in the base struct.
     * See here: https://stackoverflow.com/questions/52299691/generation-ecdsa-32-byte-private-key-in-java-scala
     *
     * @param keyPair - {@link KeyPair} to parse and extract ECC secret bytes
     * @return - {@link Byte} secret key of length {@link com.github.nitram509.jmacaroons.MacaroonsConstants#MACAROON_SECRET_KEY_BYTES}
     */
    public static byte[] unwrapPrivateKeyBytes(Curve25519KeyPair keyPair) {
        return keyPair.getPrivateKey();
    }

    public static byte[] getPublicKeyBytes(Curve25519KeyPair keyPair) {
        return keyPair.getPublicKey();
    }

    private static KeyPairGenerator initialize() {
        Provider bcProvider = new BouncyCastleProvider();
        final KeyPairGenerator generator;
        try {
            final org.bouncycastle.jce.spec.ECParameterSpec spec = getCurveSpec();
            generator = KeyPairGenerator.getInstance("ECDH", bcProvider);
//            generator.initialize(163);
            generator.initialize(spec, new SecureRandom());
            return generator;
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new BakeryException("Cannot create default keypair", e);
        }
    }

    private static org.bouncycastle.jce.spec.ECParameterSpec getCurveSpec() {
        X9ECParameters ecP = CustomNamedCurves.getByName("curve25519");
        return new org.bouncycastle.jce.spec.ECParameterSpec(ecP.getCurve(), ecP.getG(),
                ecP.getN(), ecP.getH(), ecP.getSeed());
    }
}
