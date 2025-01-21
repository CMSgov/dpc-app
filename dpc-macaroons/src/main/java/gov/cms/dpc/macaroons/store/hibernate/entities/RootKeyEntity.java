package gov.cms.dpc.macaroons.store.hibernate.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotEmpty;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Hibernate model for persisting root keys
 */
@Entity(name = "root_keys")
public class RootKeyEntity implements Serializable {

    private static final long serialVersionUID = 42L;

    @Id
    private String id;

    @NotEmpty
    @Column(name = "key")
    private String rootKey;

    private OffsetDateTime created;
    private OffsetDateTime expires;

    public RootKeyEntity() {
        // Hibernate required
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRootKey() {
        return rootKey;
    }

    public void setRootKey(String rootKey) {
        this.rootKey = rootKey;
    }

    public OffsetDateTime getCreated() {
        return created;
    }

    public void setCreated(OffsetDateTime created) {
        this.created = created;
    }

    public OffsetDateTime getExpires() {
        return expires;
    }

    public void setExpires(OffsetDateTime expires) {
        this.expires = expires;
    }
}
