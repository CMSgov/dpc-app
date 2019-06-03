package gov.cms.dpc.common.entities;

import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRConvertable;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

public class OrganizationEntity implements Serializable, FHIRConvertable<OrganizationEntity, Organization> {
    public static final long serialVersionUID = 42L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Embedded
    private OrganizationID organizationID;

    @NotEmpty
    @Column(name = "id")
    private String organizationName;

    @NotEmpty
    @Column(name = "address_line")
    private String organizationAddress;

    @NotEmpty
    @Column(name = "contact_name")
    private String contactName;

    @NotEmpty
    @Column(name = "contact_email")
    private String contactEmail;

    @NotEmpty
    @Column(name = "contact_phone")
    private String contactPhone;

    OrganizationEntity() {
        // Not used
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OrganizationID getOrganizationID() {
        return organizationID;
    }

    public void setOrganizationID(OrganizationID organizationID) {
        this.organizationID = organizationID;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationAddress() {
        return organizationAddress;
    }

    public void setOrganizationAddress(String organizationAddress) {
        this.organizationAddress = organizationAddress;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    @Override
    public OrganizationEntity fromFHIR(Organization resource) {
        final OrganizationEntity entity = new OrganizationEntity();

        // Find the first Organization ID that we can use
        final Optional<Identifier> identifier = resource
                .getIdentifier()
                .stream()
                // Don't support UNKNOWN systems for now, only things we can use
                .filter(id -> {
                    final String system = id.getSystem();
                    // If it's null, we can use it
                    try {
                        final DPCIdentifierSystem idSys = DPCIdentifierSystem.fromString(system);
                        // MPI does not work
                        return idSys != DPCIdentifierSystem.MBI;
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst();

        if (identifier.isEmpty()) {
            throw new DataFormatException("Identifier must be NPPES or PECOS");
        }
        entity.setOrganizationID(new OrganizationID(
                DPCIdentifierSystem.fromString(identifier.get().getSystem()),
                identifier.get().getValue()));

        entity.setOrganizationName(resource.getName());
        entity.setOrganizationAddress(resource.getAddressFirstRep().getLine().get(0).toString());
        // Find contact info
        entity.setContactName(findContactType(resource,
                Organization.OrganizationContactComponent::hasName).toString());

        entity.setContactPhone(findContactType(resource,
                (contact) -> (contact.hasTelecom()
                        && contact.getTelecom().stream()
                        .anyMatch(tel -> tel.getSystem() == ContactPoint.ContactPointSystem.PHONE))).toString());

        entity.setContactEmail(findContactType(resource,
                (contact) -> (contact.hasTelecom()
                        && contact.getTelecom().stream()
                        .anyMatch(tel -> tel.getSystem() == ContactPoint.ContactPointSystem.EMAIL))).toString());

        entity.setOrganizationAddress(resource.getAddressFirstRep().getText());

        return entity;
    }

    @Override
    public Organization toFHIR() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Embeddable
    public static class OrganizationID implements Serializable {
        public static final long serialVersionUID = 42L;

        private final DPCIdentifierSystem system;
        private final String value;

        public OrganizationID(DPCIdentifierSystem system, String value) {
            this.system = system;
            this.value = value;
        }

        public DPCIdentifierSystem getSystem() {
            return system;
        }

        public String getValue() {
            return value;
        }
    }

    private static Organization.OrganizationContactComponent findContactType(Organization resource, Predicate<Organization.OrganizationContactComponent> predicate) {
        return resource
                .getContact()
                .stream()
                .filter(predicate)
                .findAny()
                .orElseThrow(() -> new DataFormatException("Cannot find required contact type for organization"));
    }
}
