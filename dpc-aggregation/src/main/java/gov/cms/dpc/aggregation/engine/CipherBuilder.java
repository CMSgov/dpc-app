package gov.cms.dpc.aggregation.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.security.auth.DestroyFailedException;
import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to build the CipherWriter and it associated key materials.
 */
public class CipherBuilder implements AutoCloseable {
    private SecretKey secretKey = null;
    private byte[] iv;
    private final String symmetricCipher;
    private final String asymmetricCipher;
    private final int keyBits;
    private final int ivBits;
    private final int gcmTagLength;

    /**
     * The following set of symmetric parameters follows NIST SP800-38D and NIST SP800-57
     * as well as expectations from our web site.
     */
    public static final String NIST_SYMMETRIC_CIPHER = "AES/GCM/NoPadding";
    public static final int NIST_KEY_BITS = 128;
    public static final int NIST_IV_BITS = 96;
    public static final int NIST_GCM_TAG_LENGTH = 128;

    /**
     * Create ciphers according to the passed in config
     */
    CipherBuilder() {
        symmetricCipher = NIST_SYMMETRIC_CIPHER;
        keyBits = NIST_KEY_BITS;
        ivBits = NIST_IV_BITS;
        gcmTagLength = NIST_GCM_TAG_LENGTH;
        asymmetricCipher = "RSA/ECB/PKCS1Padding"; // Will want to enventually support more types
    }

    /**
     * Generate the keyMaterials needed to form a cipher. Should be called first.
     *
     * @throws GeneralSecurityException for config errors
     */
    void generateKeyMaterial() throws GeneralSecurityException {
        if (secretKey != null) {
            this.close();
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(symmetricCipher.split("/", -1)[0]);
        keyGenerator.init(keyBits);
        secretKey = keyGenerator.generateKey();

        SecureRandom secureRandom = new SecureRandom();
        iv = new byte[ivBits / 8];
        secureRandom.nextBytes(iv);
    }

    /**
     * Form a one-use cipher for the previously generated key materials.
     *
     * @return a one use cipher
     * @throws GeneralSecurityException on configuration errors
     */
    Cipher formCipher() throws GeneralSecurityException {
        assert(secretKey != null);
        final var aesCipher = Cipher.getInstance(symmetricCipher);

        aesCipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(gcmTagLength, iv));
        return aesCipher;
    }

    /**
     * Encodes the generated key material into a JSON metadata structure in the following format:
     * {
     * "SymmetricProperties" : {
     * "Cipher" : (String) AES cipher type (for consumption by javax.crypto.Cipher.getInstance(...)),
     * "EncryptedKey" : (String) AES secret - encrypted, then base64-encoded,
     * "InitializationVector" : (String) AES IV - base64-encoded,
     * "TagLength" : (int) GCM tag length, if using AES/GCM
     * },
     * "AsymmetricProperties" : {
     * "Cipher" : (String) RSA cipher type (for consumption by javax.crypto.Cipher.getInstance(...)),
     * "PublicKey" : (String) Base64-encoded RSA public key that was initially provided by the vendor
     * }
     * }
     *
     * @param rsaPublicKey - the public key used encrypt symmetric key used for encryption
     * @return JSON formatted string
     * @throws GeneralSecurityException for config errors
     * @throws IOException for JSON errors
     */
    String getMetadata(byte[] rsaPublicKey) throws GeneralSecurityException, IOException {
        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        Cipher rsaCipher = Cipher.getInstance(asymmetricCipher);
        rsaCipher.init(
                Cipher.ENCRYPT_MODE,
                rsaKeyFactory.generatePublic(new X509EncodedKeySpec(rsaPublicKey))
        );

        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> symmetricMetadata = new HashMap<>();
        Map<String, Object> asymmetricMetadata = new HashMap<>();

        symmetricMetadata.put("Cipher", symmetricCipher);
        symmetricMetadata.put("EncryptedKey", Base64.getEncoder().encodeToString(rsaCipher.doFinal(secretKey.getEncoded())));
        symmetricMetadata.put("InitializationVector", Base64.getEncoder().encodeToString(iv));
        symmetricMetadata.put("TagLength", gcmTagLength);

        asymmetricMetadata.put("Cipher", asymmetricCipher);
        asymmetricMetadata.put("PublicKey", Base64.getEncoder().encodeToString(rsaPublicKey));

        metadata.put("SymmetricProperties", symmetricMetadata);
        metadata.put("AsymmetricProperties", asymmetricMetadata);

        return new ObjectMapper().writeValueAsString(metadata);
    }

    @Override
    public void close() {
        // Remove key material from memory when we're done.
        //
        // Unfortunately, calling secretKey.destroy(); will throw DestroyFailedException
        // As of Apr 2019, the feature wasn't implemented in OpenJDK (ref: https://bugs.openjdk.java.net/browse/JDK-8160206)
        try {
            if (secretKey != null) {
                secretKey.destroy();
                secretKey = null;
            }
        } catch(DestroyFailedException ex) {
            // eat the exception (at least we tried)
        }
    }
}
