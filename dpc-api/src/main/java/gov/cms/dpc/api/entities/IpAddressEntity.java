package gov.cms.dpc.api.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.cms.dpc.common.converters.jackson.OffsetDateTimeToStringConverter;
import gov.cms.dpc.common.converters.jackson.StringToOffsetDateTimeConverter;
import io.hypersistence.utils.hibernate.type.basic.Inet;
import io.hypersistence.utils.hibernate.type.basic.PostgreSQLInetType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TypeDef;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity(name = "ip_address")
@TypeDef(typeClass = PostgreSQLInetType.class, defaultForType = Inet.class)
public class IpAddressEntity implements Serializable {
    public static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    @Column(name = "ip_address_id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID ipAddressId;

    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    private UUID organizationId;

    @Column(name = "ip_address", nullable = false, columnDefinition = "inet")
    private Inet ipAddress;

    @NotNull
    private String label;

    @Column(name = "created_at", columnDefinition = "timestamp with time zone")
    @JsonSerialize(converter = OffsetDateTimeToStringConverter.class)
    @JsonDeserialize(converter = StringToOffsetDateTimeConverter.class)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    public UUID getIpAddressId() {return ipAddressId;}
    public void setOrganizationId(UUID organizationId) {this.organizationId = organizationId;}
    public UUID getOrganizationId() {return organizationId;}
    public void setIpAddress(Inet ipAddress) {this.ipAddress = ipAddress;}
    public Inet getIpAddress() {return ipAddress;}
    public void setLabel(String label) {this.label = label;}
    public String getLabel() {return label;}
    public OffsetDateTime getCreatedAt() {return createdAt;}
}
