package gov.cms.dpc.api.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.api.converters.InetDeserializer;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import gov.cms.dpc.common.converters.jackson.StringToOffsetDateTimeConverter;
import io.hypersistence.utils.hibernate.type.basic.Inet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity(name = "ip_addresses")
public class IpAddressEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    private UUID organizationId;

    @Column(name = "ip_address", nullable = false, columnDefinition = "inet")
    @JsonDeserialize(using = InetDeserializer.class)
    private Inet ipAddress;

    @NotNull
    private String label;

    @Column(name = "created_at", columnDefinition = "timestamp with time zone")
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    public UUID getId() {return id;}
    public IpAddressEntity setId(UUID id) {this.id = id; return this;}
    public IpAddressEntity setOrganizationId(UUID organizationId) {this.organizationId = organizationId; return this;}
    public UUID getOrganizationId() {return organizationId;}
    public IpAddressEntity setIpAddress(Inet ipAddress) {this.ipAddress = ipAddress; return this;}
    public Inet getIpAddress() {return ipAddress;}
    public IpAddressEntity setLabel(String label) {this.label = label; return this;}
    public String getLabel() {return label;}
    public OffsetDateTime getCreatedAt() {return createdAt;}
}
