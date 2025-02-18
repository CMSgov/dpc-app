package gov.cms.dpc.testing.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@SuppressWarnings("WeakerAccess")
public class KeyView implements Serializable {
    @Serial
    private static final long serialVersionUID = 42L;

    public UUID id;
    public String publicKey;
    @JsonDeserialize(converter = TimeConverter.class)
    public OffsetDateTime createdAt;
    public String label;

    KeyView() {
        // Not used
    }

    // Duplicating StringToOffsetDateTimeConverter, in order to avoid pulling dpc-common in as a dependency.
    // It's a simple class, nothing too exciting
    public static class TimeConverter extends StdConverter<String, OffsetDateTime> {

        @Override
        public OffsetDateTime convert(String s) {
            return OffsetDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx"));
        }
    }
}
