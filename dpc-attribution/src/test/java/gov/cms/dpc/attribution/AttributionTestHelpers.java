package gov.cms.dpc.attribution;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import gov.cms.dpc.common.entities.*;
import gov.cms.dpc.common.utils.NPIUtil;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.*;

import java.sql.Date;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class AttributionTestHelpers {

    public static final String DEFAULT_ORG_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    public static final String DEFAULT_PATIENT_BENE_ID = "-19990000002901";
    public static final String DEFAULT_PATIENT_MBI = "3SQ0C00AA00";

    public static Practitioner createPractitionerResource(String NPI) {
        final Practitioner practitioner = new Practitioner();
        practitioner.addIdentifier().setValue(NPI).setSystem(DPCIdentifierSystem.NPPES.getSystem());
        practitioner.addName()
                .setFamily("Practitioner").addGiven("Test");

        // Meta data which includes the Org we're using
        final Meta meta = new Meta();
        meta.addTag(DPCIdentifierSystem.DPC.getSystem(), DEFAULT_ORG_ID, "OrganizationID");
        practitioner.setMeta(meta);

        return practitioner;
    }

    public static Patient createPatientResource(String MBI, String organizationID) {
        final Patient patient = new Patient();
        patient.addIdentifier()
                .setSystem(DPCIdentifierSystem.MBI.getSystem())
                .setValue(MBI);

        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
        patient.setGender(Enumerations.AdministrativeGender.OTHER);
        patient.setManagingOrganization(new Reference(new IdType("Organization", organizationID)));

        return patient;
    }

    public static Organization createOrgResource(String uuid, String npi){
        final Organization organization = new Organization();
        organization.setId(uuid);
        organization.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue(npi);
        organization.setName("Test Org");
        organization.addAddress().addLine("12345 Fake Street");
        return organization;
    }

    public static Bundle createBundle(Resource... resources){
        final Bundle bundle = new Bundle();
        if(resources != null){
            for(Resource resource:resources){
                bundle.addEntry(new Bundle.BundleEntryComponent().setResource(resource));
            }
        }
        return bundle;
    }

    public static IGenericClient createFHIRClient(FhirContext ctx, String serverURL) {
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
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

        EndpointEntity endpointEntity = new EndpointEntity();
        endpointEntity.setId(UUID.randomUUID());
        endpointEntity.setName("endpoint");
        endpointEntity.setStatus(Endpoint.EndpointStatus.ACTIVE);
        endpointEntity.setAddress("http://www.endpoint.com");

        OrganizationEntity.OrganizationID orgEntId = new OrganizationEntity.OrganizationID(DPCIdentifierSystem.NPPES, NPIUtil.generateNPI());
        OrganizationEntity organizationEntity = new OrganizationEntity();
        organizationEntity.setId(UUID.randomUUID());
        organizationEntity.setOrganizationID(orgEntId);
        organizationEntity.setOrganizationName("Name");
        organizationEntity.setOrganizationAddress(addressEntity);
        organizationEntity.setContacts(List.of(contactEntity));
        organizationEntity.setEndpoints(List.of(endpointEntity));

        return organizationEntity;
    }

    public static PatientEntity createPatientEntity(OrganizationEntity org, String mbi) {
        PatientEntity patientEntity = new PatientEntity();
        patientEntity.setID(UUID.randomUUID());
        patientEntity.setBeneficiaryID(mbi);
        patientEntity.setDob(LocalDate.of(1980, 7, 14));
        patientEntity.setGender(Enumerations.AdministrativeGender.MALE);
        patientEntity.setOrganization(org);

        return patientEntity;
    }

    public static ProviderEntity createProviderEntity(OrganizationEntity org) {
        ProviderEntity providerEntity = new ProviderEntity();
        providerEntity.setID(UUID.randomUUID());
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
