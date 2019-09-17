package gov.cms.dpc.api.auth;

import gov.cms.dpc.api.exceptions.PublicKeyException;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class PublicKeyHandler {

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
}
