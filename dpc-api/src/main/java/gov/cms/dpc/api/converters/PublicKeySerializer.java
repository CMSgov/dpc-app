package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.IOException;
import java.io.StringWriter;

/**
 * {@link StdConverter} that serializes the given {@link SubjectPublicKeyInfo} as a PEM encoded {@link String}
 */
public class PublicKeySerializer extends StdConverter<SubjectPublicKeyInfo, String> {

    PublicKeySerializer() {
        // Not used
    }

    @Override
    public String convert(SubjectPublicKeyInfo value) {
        try {
            final PemObject object = new PemObject("PUBLIC KEY", value.getEncoded());
            try (StringWriter stringWriter = new StringWriter()) {
                try (PemWriter pemWriter = new PemWriter(stringWriter)) {
                    pemWriter.writeObject(object);
                }
                return stringWriter.toString();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot convert public key to PEM", e);
        }
    }
}
