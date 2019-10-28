package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.Base64;


/**
 * Converts a {@link String} to a byte array using {@link Base64#getMimeDecoder()}.
 * Useful for public key contents
 */
public class MimeToKeyBytesConverter extends StdConverter<String, byte[]> {

    private final Base64.Decoder decoder;

    private MimeToKeyBytesConverter() {
        this.decoder = Base64.getMimeDecoder();
    }

    @Override
    public byte[] convert(String value) {
        return this.decoder.decode(value);
    }
}
