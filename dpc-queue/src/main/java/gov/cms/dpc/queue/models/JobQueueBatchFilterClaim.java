package gov.cms.dpc.queue.models;

import gov.cms.dpc.fhir.DPCResourceType;
import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single filter claim (e.g. a resource-level attribute filter) applied to a v3 job batch.
 */
@Entity
@Table(name = "job_queue_batch_filter_claims")
public class JobQueueBatchFilterClaim implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "batch_id")
    private UUID batchID;

    @Column(name = "job_id")
    private UUID jobID;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private DPCResourceType resourceType;

    @Column(name = "filter_key")
    private String filterKey;

    @Column(name = "operator")
    private String operator;

    @Column(name = "filter_value")
    private String filterValue;

    public JobQueueBatchFilterClaim() {
        // not used
    }

    public JobQueueBatchFilterClaim(UUID batchID, UUID jobID, DPCResourceType resourceType,
                                    String filterKey, String operator, String filterValue) {
        this.id = UUID.randomUUID();
        this.batchID = batchID;
        this.jobID = jobID;
        this.resourceType = resourceType;
        this.filterKey = filterKey;
        this.operator = operator;
        this.filterValue = filterValue;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBatchID() {
        return batchID;
    }

    public UUID getJobID() {
        return jobID;
    }

    public DPCResourceType getResourceType() {
        return resourceType;
    }

    public String getFilterKey() {
        return filterKey;
    }

    public String getOperator() {
        return operator;
    }

    public String getFilterValue() {
        return filterValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobQueueBatchFilterClaim that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(batchID, that.batchID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, batchID);
    }

    @Override
    public String toString() {
        return "JobQueueBatchFilterClaim{" +
                "id=" + id +
                ", batchID=" + batchID +
                ", jobID=" + jobID +
                ", resourceType=" + resourceType +
                ", filterKey='" + filterKey + '\'' +
                ", operator='" + operator + '\'' +
                ", filterValue='" + filterValue + '\'' +
                '}';
    }
}
