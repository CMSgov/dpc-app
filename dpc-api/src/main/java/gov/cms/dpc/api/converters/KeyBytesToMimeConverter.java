package gov.cms.dpc.api.converters;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.Base64;

/**
 * Converts a byte array to a {@link String} using {@link Base64#getMimeEncoder()}.
 * Useful for public key contents
 */
public class KeyBytesToMimeConverter extends StdConverter<byte[], String> {

    private final Base64.Encoder encoder;

    private KeyBytesToMimeConverter() {
        this.encoder = Base64.getMimeEncoder();
    }

    @Override
    public String convert(byte[] value) {
        return this.encoder.encodeToString(value);
    }
}
