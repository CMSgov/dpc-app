package gov.cms.dpc.api.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.common.annotations.NoHtml;
import gov.cms.dpc.common.converters.jackson.MultiFormatOffsetDateTimeDeserializer;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;


public class CreateTokenRequest implements Serializable {

    private static final long serialVersionUID = 4464861101748469238L;

    @NoHtml
    private String label;

    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = MultiFormatOffsetDateTimeDeserializer.class)
    private OffsetDateTime expiresAt;

    public CreateTokenRequest() {
        // Jackson required
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CreateTokenRequest)) return false;
        CreateTokenRequest that = (CreateTokenRequest) o;
        return Objects.equals(label, that.label) &&
                Objects.equals(expiresAt, that.expiresAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, expiresAt);
    }
}
