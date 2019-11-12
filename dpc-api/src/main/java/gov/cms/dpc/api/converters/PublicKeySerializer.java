package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.databind.util.StdConverter;
import gov.cms.dpc.api.auth.jwt.PublicKeyHandler;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * {@link StdConverter} that serializes the given {@link SubjectPublicKeyInfo} as a PEM encoded {@link String}
 */
public class PublicKeySerializer extends StdConverter<SubjectPublicKeyInfo, String> {

    PublicKeySerializer() {
        // Not used
    }

    @Override
    public String convert(SubjectPublicKeyInfo value) {
        return PublicKeyHandler.pemEncodePublicKey(value);
    }
}
