package gov.cms.dpc.common.entities;

import gov.cms.dpc.common.annotations.NoHtml;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@MappedSuperclass
public abstract class PersonEntity implements Serializable {

    private static final long serialVersionUID = 42L;

    @NoHtml
    @Column(name = "first_name")
    protected String firstName;
    @NoHtml
    @Column(name = "last_name")
    protected String lastName;
    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    protected OffsetDateTime createdAt;
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    protected OffsetDateTime updatedAt;

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    protected UUID id;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public UUID getID() {
        return id;
    }

    public void setID(UUID id) {
        this.id = id;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void setCreation() {
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.setCreatedAt(now);
        this.setUpdatedAt(now);
    }

    @PreUpdate
    public void setUpdateTime() {
        this.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }
}
