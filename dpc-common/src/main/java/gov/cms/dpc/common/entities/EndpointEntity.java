package gov.cms.dpc.common.entities;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.Endpoint;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@Entity(name = "organization_endpoints")
public class EndpointEntity implements Serializable {

    public enum ValidationStatus {
        NONE,
        PENDING,
        VALIDATED,
        FAILED
    }

    public static final long serialVersionUID = 42L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne
    private OrganizationEntity organization;

    @NotNull
    private Endpoint.EndpointStatus status;
    @Valid
    @Embedded
    private ConnectionType connectionType;
    @NotEmpty
    private String name;
    @NotEmpty
    private String address;
    @NotNull
    @Column(name = "validation_status")
    private ValidationStatus validationStatus;
    @Column(name = "validation_message")
    private String validationMessage;

    public EndpointEntity() {
        this.validationMessage = "";
        this.validationStatus = ValidationStatus.NONE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Endpoint.EndpointStatus getStatus() {
        return status;
    }

    public void setStatus(Endpoint.EndpointStatus status) {
        this.status = status;
    }

    public ConnectionType getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(ConnectionType connectionType) {
        this.connectionType = connectionType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public OrganizationEntity getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationEntity organization) {
        this.organization = organization;
    }

    public ValidationStatus getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    @Embeddable
    public static class ConnectionType {

        @NotEmpty
        private String system;
        @NotEmpty
        private String code;

        public ConnectionType() {
            // Hibernate required
        }

        public String getSystem() {
            return system;
        }

        public void setSystem(String system) {
            this.system = system;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }


}
