package gov.cms.dpc.attribution;

import gov.cms.dpc.common.entities.r4.*;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.utils.MBIUtil;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AttributionTestHelpers {

    public static OrganizationEntity createOrganizationEntity() {
        AddressEntity addressEntity = new AddressEntity();
        addressEntity.setType(Address.AddressType.POSTAL);
        addressEntity.setUse(Address.AddressUse.HOME);
        addressEntity.setLine1("123 Test Street");

        NameEntity nameEntity = new NameEntity();
        nameEntity.setGiven("given");
        nameEntity.setFamily("family");
        nameEntity.setUse(HumanName.NameUse.OFFICIAL);

        ContactEntity contactEntity = new ContactEntity();
        contactEntity.setName(nameEntity);
        contactEntity.setAddress(addressEntity);
        contactEntity.setTelecom(List.of());

        OrganizationEntity.OrganizationID orgEntId = new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES, NPIUtil.generateNPI());
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(UUID.randomUUID());
        organizationEntity.setOrganizationID(orgEntId);
        organizationEntity.setOrganizationName("Name");
        organizationEntity.setOrganizationAddress(addressEntity);
        organizationEntity.setContacts(List.of(contactEntity));

        return organizationEntity;
    }

    public static PatientEntity createPatientEntity(OrganizationEntity org) {
        PatientEntity patientEntity = new PatientEntity();
        patientEntity.setBeneficiaryID(MBIUtil.generateMBI());
        patientEntity.setDob(LocalDate.of(1980, 7, 14));
        patientEntity.setGender(Enumerations.AdministrativeGender.MALE);
        patientEntity.setOrganization(org);

        return patientEntity;
    }

    public static ProviderEntity createProviderEntity(OrganizationEntity org) {
        ProviderEntity providerEntity = new ProviderEntity();

        providerEntity.setProviderNPI(NPIUtil.generateNPI());
        providerEntity.setOrganization(org);

        return providerEntity;
    }

    public static RosterEntity createRosterEntity(OrganizationEntity org, ProviderEntity providerEntity) {
        RosterEntity rosterEntity = new RosterEntity();
        rosterEntity.setId(UUID.randomUUID());
        rosterEntity.setManagingOrganization(org);
        rosterEntity.setAttributedProvider(providerEntity);

        return rosterEntity;
    }

    public static AttributionRelationship createAttributionRelationship(RosterEntity roster, PatientEntity patient) {
        AttributionRelationship attributionRelationship = new AttributionRelationship();
        attributionRelationship.setRoster(roster);
        attributionRelationship.setPatient(patient);
        attributionRelationship.setPeriodBegin(OffsetDateTime.now().plusMonths(1));
        attributionRelationship.setPeriodEnd(OffsetDateTime.now().minusMonths(1));

        return attributionRelationship;
    }

}
