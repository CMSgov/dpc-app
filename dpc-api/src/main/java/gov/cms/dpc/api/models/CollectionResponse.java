package gov.cms.dpc.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import gov.cms.dpc.common.converters.jackson.StringToOffsetDateTimeConverter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;

/**
 * Wrapper method for enclosing a given {@link Collection} of {@link T} with some additional metadata.
 * @param <T> - type parameter of encompassed class
 */
public class CollectionResponse<T extends Serializable> implements Serializable {
    public static final long serialVersionUID = 42L;

    private Collection<T> entities;
    private int count;

    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonProperty(value = "created_at")
    private OffsetDateTime createdAt;

    private CollectionResponse() {
        // Jackson required
    }

    public CollectionResponse(Collection<T> entities) {
        this.entities = entities;
        this.count = entities.size();
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public Collection<T> getEntities() {
        return entities;
    }

    public void setEntities(Collection<T> entities) {
        this.entities = entities;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
