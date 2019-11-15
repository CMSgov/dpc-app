package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import gov.cms.dpc.api.auth.jwt.PublicKeyHandler;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * {@link StdConverter} that de-serializes a {@link SubjectPublicKeyInfo} from a PEM encoded {@link String}
 *
 * Primarily used for testing and debugging.
 */
public class PublicKeyDeserializer extends StdConverter<String, SubjectPublicKeyInfo> {

    PublicKeyDeserializer() {
        // Not used
    }

    @Override
    public SubjectPublicKeyInfo convert(String value) {
        return PublicKeyHandler.parsePEMString(value);
    }
}
