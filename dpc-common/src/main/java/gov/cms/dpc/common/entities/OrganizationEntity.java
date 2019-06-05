package gov.cms.dpc.common.entities;

import ca.uhn.fhir.parser.DataFormatException;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRConvertable;
import gov.cms.dpc.fhir.converters.AddressConverter;
import gov.cms.dpc.fhir.converters.ContactElementConverter;
import org.hibernate.validator.constraints.NotEmpty;
import org.hl7.fhir.dstu3.model.ContactPoint;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Organization;

import javax.persistence.*;
import javax.validation.Valid;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Entity(name = "organizations")
public class OrganizationEntity implements Serializable, FHIRConvertable<OrganizationEntity, Organization> {
    public static final long serialVersionUID = 42L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Embedded
    private OrganizationID organizationID;

    @NotEmpty
    @Column(name = "organization_name")
    private String organizationName;

    @Valid
    @Embedded
    private AddressEntity organizationAddress;

//    @Valid
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "organization")
    private List<ContactEntity> contacts;

    public OrganizationEntity() {
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

    public AddressEntity getOrganizationAddress() {
        return organizationAddress;
    }

    public void setOrganizationAddress(AddressEntity organizationAddress) {
        this.organizationAddress = organizationAddress;
    }

    public List<ContactEntity> getContacts() {
        return contacts;
    }

    public void setContacts(List<ContactEntity> contacts) {
        this.contacts = contacts;
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
        entity.setOrganizationAddress(AddressConverter.convert(resource.getAddressFirstRep()));

        // Add all contact info
        final List<ContactEntity> contacts = resource
                .getContact()
                .stream()
                .map(ContactElementConverter::convert)
                .collect(Collectors.toList());
        entity.setContacts(contacts);

        return entity;
    }

    @Override
    public Organization toFHIR() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Embeddable
    public static class OrganizationID implements Serializable {
        public static final long serialVersionUID = 42L;

        @Column(name = "id_system")
        private DPCIdentifierSystem system;

        @NotEmpty
        @Column(name = "id_value")
        private String value;

        public OrganizationID() {
            // Hibernate Required
        }

        public OrganizationID(DPCIdentifierSystem system, String value) {
            this.system = system;
            this.value = value;
        }

        public DPCIdentifierSystem getSystem() {
            return system;
        }

        public void setSystem(DPCIdentifierSystem system) {
            this.system = system;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static Organization.OrganizationContactComponent findContactType(Organization resource, Predicate<Organization.OrganizationContactComponent> predicate) {
        return resource
                .getContact()
                .stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new DataFormatException("Cannot find required contact type for organization"));
    }

    private static String extractContactValue(Organization.OrganizationContactComponent component, ContactPoint.ContactPointSystem system) {
        final Optional<ContactPoint> first = component
                .getTelecom()
                .stream()
                .filter(tel -> tel.getSystem() == system)
                .findFirst();

        return first
                .orElseThrow(() -> new DataFormatException(String.format("Cannot find contact type: %s", system)))
                .getValue();
    }
}
