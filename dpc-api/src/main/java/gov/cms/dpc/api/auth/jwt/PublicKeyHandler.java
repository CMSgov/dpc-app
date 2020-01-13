package gov.cms.dpc.api.auth.jwt;

import gov.cms.dpc.api.entities.PublicKeyEntity;
import gov.cms.dpc.api.exceptions.PublicKeyException;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class PublicKeyHandler {

    static final ASN1ObjectIdentifier RSA_PARENT = new ASN1ObjectIdentifier("1.2.840.113549");
    static final ASN1ObjectIdentifier ECC_KEY = new ASN1ObjectIdentifier("1.2.840.10045.2.1");
    // ECC Curve names are defined here: https://tools.ietf.org/search/rfc4492#section-5.1.1
    // They're in two separate namespaces, certicom and ansi-x962
    static final ASN1ObjectIdentifier SECP256_KEY = new ASN1ObjectIdentifier("1.2.840.10045.3.1.7");
    static final ASN1ObjectIdentifier SECP384_KEY = new ASN1ObjectIdentifier("1.3.132.0.34");

    private PublicKeyHandler() {
        // Not used
    }

    /**
     * Parse and validate PEM encoded Public Key
     *
     * @param pem - {@link String} PEM encoded public key
     * @return - {@link SubjectPublicKeyInfo}
     */
    public static SubjectPublicKeyInfo parsePEMString(String pem) {
        final ByteArrayInputStream bas = new ByteArrayInputStream(pem.getBytes(StandardCharsets.ISO_8859_1));
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bas, StandardCharsets.UTF_8))) {
            try (PEMParser pemParser = new PEMParser(bufferedReader)) {
                try {
                    final Object object = pemParser.readObject();
                    if (object == null) {
                        throw new PublicKeyException("Cannot parse public key, returned value is null");
                    }
                    if (!(object instanceof SubjectPublicKeyInfo)) {
                        throw new PublicKeyException(String.format("Cannot convert %s to %s.", object.getClass().getName(), SubjectPublicKeyInfo.class.getName()));
                    }
                    return (SubjectPublicKeyInfo) object;
                } catch (PEMException e) {
                    throw new PublicKeyException("Not a valid public key", e);
                }
            }
        } catch (IOException e) {
            throw new PublicKeyException("Cannot parse Public Key input", e);
        }
    }

    /**
     * Convert the {@link SubjectPublicKeyInfo} to a PEM encoded string value
     *
     * @param value - {@link SubjectPublicKeyInfo} to encode as PEM
     * @return - {@link String} PEM encoded public key
     */
    public static String pemEncodePublicKey(SubjectPublicKeyInfo value) {
        try {
            final PemObject object = new PemObject("PUBLIC KEY", value.getEncoded());
            try (StringWriter stringWriter = new StringWriter()) {
                try (PemWriter pemWriter = new PemWriter(stringWriter)) {
                    pemWriter.writeObject(object);
                }
                return stringWriter.toString();
            }
        } catch (IOException e) {
            throw new PublicKeyException("Cannot convert public key to PEM", e);
        }
    }

    public static void validatePublicKey(SubjectPublicKeyInfo value) {
        // If RSA, do some other validations
        final ASN1ObjectIdentifier algorithmID = value.getAlgorithm().getAlgorithm();
        if (algorithmID.on(RSA_PARENT)) {
            validateRSAKey(value);
        } else if (algorithmID.equals(ECC_KEY)) {
            validateECCKey(value);
        } else {
            throw new PublicKeyException(String.format("Unsupported key type `%s`.", algorithmID.getId()));
        }

    }

    private static void validateRSAKey(SubjectPublicKeyInfo value) {
        // Should have a minimum length
        try {
            // Verifies the key is at least 4096 bits, which is 550 bytes of encoded data
            if (value.getEncoded().length < 550) {
                throw new PublicKeyException("Public key must be at least 4096 bits.");
            }
        } catch (IOException e) {
            throw new PublicKeyException("Cannot read public key.", e);
        }

    }

    private static void validateECCKey(SubjectPublicKeyInfo value) {
        // Verify we have a supported curve, which is currently secp256r1 or secp384r1
        final ASN1Primitive curveName = value.getAlgorithm().getParameters().toASN1Primitive();
        if (!(curveName.equals(SECP256_KEY) || curveName.equals(SECP384_KEY))) {
            throw new PublicKeyException(String.format("ECC curve `%s` is not supported.", curveName.toString()));
        }
    }

    /**
     * Convert the given {@link PublicKeyEntity} to a {@link PublicKey}
     *
     * @param entity - {@link PublicKeyEntity} to convert
     * @return - {@link PublicKey}
     * @throws PublicKeyException - throws if the conversion fails
     */
    static PublicKey publicKeyFromEntity(PublicKeyEntity entity) {
        X509EncodedKeySpec spec;
        try {
            final SubjectPublicKeyInfo publicKeySpec = entity.getPublicKey();
            final String keyType = publicKeySpec.getAlgorithm().getAlgorithm().on(RSA_PARENT) ? "RSA" : "EC";
            spec = new X509EncodedKeySpec(publicKeySpec.getEncoded());
            return KeyFactory.getInstance(keyType).generatePublic(spec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new PublicKeyException("Cannot convert Key Spec to Public Key", e);
        }
    }
}
