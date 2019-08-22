package gov.cms.dpc.macaroons.helpers;

import com.github.nitram509.jmacaroons.MacaroonsConstants;
import gov.cms.dpc.macaroons.exceptions.BakeryException;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.ec.CustomNamedCurves;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.whispersystems.curve25519.java.curve_sigs;

import java.security.*;
import java.security.interfaces.ECPrivateKey;

/**
 * Helper factory for
 */
public class BakeryKeyFactory {

    private static final KeyPairGenerator generator = initialize();

    public static KeyPair generateKeyPair() {
        return generator.generateKeyPair();
    }

    /**
     * Unwrap the raw key bytes from the underlying {@link java.security.interfaces.ECPrivateKey}.
     * We need this because BounceyCastle encodes a lot of additional information in the base struct.
     * See here: https://stackoverflow.com/questions/52299691/generation-ecdsa-32-byte-private-key-in-java-scala
     * @param keyPair
     * @return
     */
    public static byte[] unwrapPrivateKeyBytes(KeyPair keyPair) {
        return ((ECPrivateKey) keyPair.getPrivate()).getS().toByteArray();
    }

    public static byte[] unwrapPublicKeyBytes(byte[] privateKeyBytes) {

        final byte[] pubKeyOuts = new byte[MacaroonsConstants.MACAROON_SECRET_KEY_BYTES];
        curve_sigs.curve25519_keygen(pubKeyOuts, privateKeyBytes);

        return pubKeyOuts;
    }

    private static KeyPairGenerator initialize() {
        Provider bcProvider = new BouncyCastleProvider();
        final KeyPairGenerator generator;
        try {
            final ECParameterSpec spec = getCurveSpec();
            generator = KeyPairGenerator.getInstance("ECDH", bcProvider);
            generator.initialize(spec, new SecureRandom());
            return generator;
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new BakeryException("Cannot create default keypair", e);
        }
    }

    private static ECParameterSpec getCurveSpec() {
        X9ECParameters ecP = CustomNamedCurves.getByName("curve25519");
        return new ECParameterSpec(ecP.getCurve(), ecP.getG(),
                ecP.getN(), ecP.getH(), ecP.getSeed());
    }
}
