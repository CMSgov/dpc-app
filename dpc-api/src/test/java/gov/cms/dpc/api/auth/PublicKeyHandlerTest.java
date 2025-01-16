package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.auth.jwt.PublicKeyHandler;
import gov.cms.dpc.api.exceptions.PublicKeyException;
import gov.cms.dpc.testing.APIAuthHelpers;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import gov.cms.dpc.testing.KeyType;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("Public Key Handler Tests")
@SuppressWarnings("InnerClassMayBeStatic")
class PublicKeyHandlerTest {

    @Nested
    @DisplayName("Public Key Parsing Tests")
    class KeyParsingTests {


        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testValidKey(KeyType keyType) throws NoSuchAlgorithmException {
            final String encoded = generatePublicKey(keyType);
            final String key = String.format("-----BEGIN PUBLIC KEY-----%n%s%n-----END PUBLIC KEY-----%n", encoded);
            PublicKeyHandler.parsePEMString(key);
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testInvalidKeyHeader(KeyType keyType) throws NoSuchAlgorithmException {
            final String encoded = generatePublicKey(keyType);
            final String key = String.format("-----BEGIN RSA PUBLIC KEY-----%n%s%n-----END RSA PUBLIC KEY-----%n", encoded);
            assertThrows(PublicKeyException.class, () -> PublicKeyHandler.parsePEMString(key));
        }

        @Test
        void testEmptyKey() {
            assertThrows(PublicKeyException.class, () -> PublicKeyHandler.parsePEMString(""));
        }

        @Test
        void testGarbageKey() {
            assertThrows(PublicKeyException.class, () -> PublicKeyHandler.parsePEMString("This is NOT a real key"));
        }

        @ParameterizedTest
        @EnumSource(KeyType.class)
        void testPrivateKey(KeyType keyType) throws NoSuchAlgorithmException {
            final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);
            final String encoded = Base64.getMimeEncoder().encodeToString(keyPair.getPrivate().getEncoded());

            final String key = String.format("-----BEGIN RSA PRIVATE KEY-----%n%s%n-----END RSA PRIVATE KEY-----%n", encoded);
            final PublicKeyException exception = assertThrows(PublicKeyException.class, () -> PublicKeyHandler.parsePEMString(key));
            assertEquals("Not a valid public key", exception.getMessage(), "Should have correct error message");
        }
    }

    @Nested
    @DisplayName("Public Key Encoding Tests")
    class PublicKeyEncodingTests {

        @Test
        void testEncodeKey() throws IOException {
            final SubjectPublicKeyInfo keyInfo = Mockito.mock(SubjectPublicKeyInfo.class);
            final byte[] keyValue = "Not a real key".getBytes(StandardCharsets.UTF_8);
            Mockito.when(keyInfo.getEncoded()).thenReturn(keyValue);
            final String publicKey = PublicKeyHandler.pemEncodePublicKey(keyInfo);
            assertEquals(String.format("-----BEGIN PUBLIC KEY-----%n%s%n-----END PUBLIC KEY-----%n", Base64.getMimeEncoder().encodeToString(keyValue)), publicKey, "Public key should have bad data");
        }

        @Test
        void testPEMEncodeIOException() throws IOException {
            final SubjectPublicKeyInfo keyInfo = Mockito.mock(SubjectPublicKeyInfo.class);
            Mockito.when(keyInfo.getEncoded()).thenThrow(IOException.class);

            final PublicKeyException exception = assertThrows(PublicKeyException.class, () -> PublicKeyHandler.pemEncodePublicKey(keyInfo), "Should throw IO exception");
            assertEquals("Cannot convert public key to PEM", exception.getMessage(), "Should have correct message");
        }
    }

    @Nested
    @DisplayName("Public Key Validation Tests")
    class PublicKeyValidationTests {

        @Test
        void testRSAKeyTooShort() throws NoSuchAlgorithmException {

            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            final KeyPair keyPair = kpg.generateKeyPair();
            final byte[] encoded = keyPair.getPublic().getEncoded();
            final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(encoded));
            final PublicKeyException exception = assertThrows(PublicKeyException.class, () -> PublicKeyHandler.validatePublicKey(publicKeyInfo));
            assertEquals("Public key must be at least 4096 bits.", exception.getMessage(), "Should have correct error message");
        }

        @Test
        void testECCKeyTooSmall() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            ECGenParameterSpec spec = new ECGenParameterSpec("secp160r1");

            final InvalidAlgorithmParameterException exception = assertThrows(InvalidAlgorithmParameterException.class, () -> kpg.initialize(spec));
            assertEquals("Curve not supported: secp160r1 (1.3.132.0.8)", exception.getMessage(), "Should have correct error message");
        }

        @Test
        void testECCWrongAlgorithm() {
            final SubjectPublicKeyInfo publicKeyInfo = Mockito.mock(SubjectPublicKeyInfo.class);
            Mockito.when(publicKeyInfo.getAlgorithm()).thenAnswer(answer -> new AlgorithmIdentifier(new ASN1ObjectIdentifier("1.2.840.10045.5")));
            final PublicKeyException exception = assertThrows(PublicKeyException.class, () -> PublicKeyHandler.validatePublicKey(publicKeyInfo));
            assertEquals("Unsupported key type `1.2.840.10045.5`.", exception.getMessage(), "Should have correct error message");
        }

        @Test
        void testVerifySignature() throws Exception {
            KeyPair keyPair = APIAuthHelpers.generateKeyPair(KeyType.RSA);
            String publicKeyStr = APIAuthHelpers.generatePublicKey(keyPair.getPublic());
            String snippet = "Verify signature test";
            String sigStr = APIAuthHelpers.signString(keyPair.getPrivate(), snippet);

            assertDoesNotThrow(() -> PublicKeyHandler.verifySignature(publicKeyStr, snippet, sigStr));
        }

        @Test
        void testVerifySignatureWrongSnippet() throws Exception {
            KeyPair keyPair = APIAuthHelpers.generateKeyPair(KeyType.RSA);
            String publicKeyStr = APIAuthHelpers.generatePublicKey(keyPair.getPublic());
            String snippet = "Verify signature test";
            String sigStr = APIAuthHelpers.signString(keyPair.getPrivate(), snippet);

            PublicKeyException pke = assertThrows(PublicKeyException.class, () -> PublicKeyHandler.verifySignature(publicKeyStr, "Not the same snippet that was signed", sigStr));
            assertEquals("Key and signature do not match", pke.getMessage());
        }

        @Test
        void testVerifySignatureMismatch() throws Exception {
            KeyPair keyPair1 = APIAuthHelpers.generateKeyPair(KeyType.RSA);
            KeyPair keyPair2 = APIAuthHelpers.generateKeyPair(KeyType.RSA);
            String publicKeyStr = APIAuthHelpers.generatePublicKey(keyPair1.getPublic());
            String snippet = "Verify signature test";
            String sigStr = APIAuthHelpers.signString(keyPair2.getPrivate(), snippet);

            PublicKeyException pke = assertThrows(PublicKeyException.class, () -> PublicKeyHandler.verifySignature(publicKeyStr, snippet, sigStr));
            assertEquals("Key and signature do not match", pke.getMessage());
        }

        @Test
        void testVerifySignatureBadSigStr() throws Exception {
            KeyPair keyPair = APIAuthHelpers.generateKeyPair(KeyType.RSA);
            String publicKeyStr = APIAuthHelpers.generatePublicKey(keyPair.getPublic());
            String snippet = "Verify signature test";
            assertThrows(PublicKeyException.class, ()  -> PublicKeyHandler.verifySignature(publicKeyStr, snippet, "hoohaw;"));
        }
    }

    @Test
    void testEncryptionRoundTrip() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, IOException, InvalidKeySpecException {
        final String plainText = "This should be encrypted";
        final Cipher cipher = Cipher.getInstance("RSA");

        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        final KeyPair keyPair = kpg.generateKeyPair();
        final String encoded = Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded());
        final String key = String.format("-----BEGIN PUBLIC KEY-----%n%s%n-----END PUBLIC KEY-----%n", encoded);
        final SubjectPublicKeyInfo publicKeyInfo = PublicKeyHandler.parsePEMString(key);

        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
        final byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));


        // Reconstruct the public key and try to decrypt
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyInfo.getEncoded());
        final PublicKey rsa = KeyFactory.getInstance("RSA").generatePublic(spec);
        cipher.init(Cipher.DECRYPT_MODE, rsa);
        final byte[] decrypted = cipher.doFinal(cipherText);
        assertEquals(plainText, new String(decrypted), "Should have matching plain text");
    }

    private String generatePublicKey(KeyType keyType) throws NoSuchAlgorithmException {
        final KeyPair keyPair = APIAuthHelpers.generateKeyPair(keyType);

        return Base64.getMimeEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }
}
