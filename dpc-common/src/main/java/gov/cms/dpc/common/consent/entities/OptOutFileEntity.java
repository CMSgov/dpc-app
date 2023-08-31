package gov.cms.dpc.common.consent.entities;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity(name = "opt_out_file")
public class OptOutFileEntity implements Serializable {
    public static final String IMPORT_STATUS_IN_PROGRESS = "In-Progress";
    public static final String IMPORT_STATUS_COMPLETED = "Completed";
    public static final String IMPORT_STATUS_FAILED = "Failed";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "timestamp", nullable = false)
    private LocalDate timestamp;

    @Column(name = "import_status")
    private String importStatus;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public LocalDate getTimestamp() { return timestamp; }

    public void setTimestamp(LocalDate timestamp) { this.timestamp = timestamp; }

    public String getImportStatus() { return importStatus; }

    public void setImportStatus(String importStatus) { this.importStatus = importStatus; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
