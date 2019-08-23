package gov.cms.dpc.macaroons.helpers;

import com.codahale.xsalsa20poly1305.Keys;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;
import java.security.interfaces.ECPrivateKey;

/**
 * Helper factory for
 */
public class BakeryKeyFactory {

    private static final byte UNCOMPRESSED_POINT_INDICATOR = 0x04;
    private static final KeyPairGenerator generator = initialize();

    public static KeyPair generateKeyPair() {
        return generator.generateKeyPair();
    }

    /**
     * Unwrap the raw key bytes from the underlying {@link java.security.interfaces.ECPrivateKey}.
     * We need this because BounceyCastle encodes a lot of additional information in the base struct.
     * See here: https://stackoverflow.com/questions/52299691/generation-ecdsa-32-byte-private-key-in-java-scala
     *
     * @param keyPair
     * @return
     */
    public static byte[] unwrapPrivateKeyBytes(KeyPair keyPair) {
        return ((ECPrivateKey) keyPair.getPrivate()).getS().toByteArray();
    }

    public static byte[] getPublicKeyBytes(KeyPair keyPair) {
        return Keys.generatePublicKey(unwrapPrivateKeyBytes(keyPair));
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
