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


public class CipherBuilder implements AutoCloseable {
    private Config config;
    private SecretKey secretKey = null;
    private byte[] iv;

    /**
     * Create ciphers according to the passed in config
     */
    public CipherBuilder(Config config) {
        this.config = config;
    }

    /**
     * Generate the keyMaterials needed to form a cipher
     *
     * @throws GeneralSecurityException for config errors
     */
    public void generateKeyMaterial() throws GeneralSecurityException {
        if (secretKey != null) {
            this.close();
        }
        String symmetricCipher = config.getString("encryption.symmetricCipher");
        int keyBits = config.getInt("encryption.keyBits");
        int ivBits = config.getInt("encryption.ivBits");

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
    public Cipher formCipher() throws GeneralSecurityException {
        assert(secretKey != null);
        String symmetricCipher = config.getString("encryption.symmetricCipher");
        final var aesCipher = Cipher.getInstance(symmetricCipher);
        int gcmTagLength = config.getInt("encryption.gcmTagLength");
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
    public String getMetadata(byte[] rsaPublicKey) throws GeneralSecurityException, IOException {
        String asymmetricCipher = config.getString("encryption.asymmetricCipher");
        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        Cipher rsaCipher = Cipher.getInstance(asymmetricCipher);
        rsaCipher.init(
                Cipher.ENCRYPT_MODE,
                rsaKeyFactory.generatePublic(new X509EncodedKeySpec(rsaPublicKey))
        );

        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> symmetricMetadata = new HashMap<>();
        Map<String, Object> asymmetricMetadata = new HashMap<>();

        String symmetricCipher = config.getString("encryption.symmetricCipher");
        int gcmTagLength = config.getInt("encryption.gcmTagLength");
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
