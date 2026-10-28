package gov.cms.dpc.attribution.utils.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import gov.cms.dpc.common.entities.r4.*;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.testing.utils.MBIUtil;
import gov.cms.dpc.fhir.r4.FHIRBuilders;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AttributionTestHelpers {

    public static final String DEFAULT_ORG_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";

    public static Organization createOrgResource(String uuid, String npi){
        final Organization organization = new Organization();
        organization.setId(uuid);
        organization.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(npi);
        organization.setName("Test Org");
        organization.addAddress().addLine("12345 Fake Street");
        return organization;
    }

    public static Practitioner createPractitionerResource(String npi) {
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue(npi).setSystem(DPCIdentifierSystem.NPPES.getSystem());
        practitioner.addName().setFamily("Practitioner").addGiven("Test");

        // Metadata which includes the Org we're using
        final Meta meta = new Meta();
        meta.addTag(DPCIdentifierSystem.DPC.getSystem(), DEFAULT_ORG_ID, "OrganizationID");
        practitioner.setMeta(meta);

        return practitioner;
    }

    public static Group createBaseAttributionGroup(String providerNPI, String organizationID) {
        final CodeableConcept attributionConcept = new CodeableConcept();
        attributionConcept.addCoding().setCode("attributed-to");

        final CodeableConcept npiConcept = new CodeableConcept();
        npiConcept.addCoding().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setCode(providerNPI);

        final Group rosterGroup = new Group();
        rosterGroup.setType(Group.GroupType.PERSON);
        rosterGroup.setActual(true);
        rosterGroup.setActive(true);
        rosterGroup.addCharacteristic()
                .setExclude(false)
                .setCode(attributionConcept)
                .setValue(npiConcept);
        FHIRBuilders.addOrganizationTag(rosterGroup, UUID.fromString(organizationID));

        return rosterGroup;
    }

    public static IGenericClient createFHIRClient(FhirContext ctx, String serverURL) {
        return createFHIRClient(ctx, serverURL, null);
    }

    public static IGenericClient createFHIRClient(FhirContext ctx, String serverURL, Integer timeout) {
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);

        // If they want a specific timeout use it, otherwise use the default.
        if (timeout != null) {
            ctx.getRestfulClientFactory().setSocketTimeout(timeout);
            ctx.getRestfulClientFactory().setConnectTimeout(timeout);
            ctx.getRestfulClientFactory().setConnectionRequestTimeout(timeout);
        }

        IGenericClient client = ctx.newRestfulGenericClient(serverURL);

        // Disable logging for tests
        LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
        loggingInterceptor.setLogRequestSummary(false);
        loggingInterceptor.setLogResponseSummary(false);
        client.registerInterceptor(loggingInterceptor);

        return client;
    }

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
