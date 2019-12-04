package gov.cms.dpc.attribution;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.hl7.fhir.dstu3.model.*;

import java.io.InputStream;
import java.sql.Date;

public class AttributionTestHelpers {

    public static final String DEFAULT_ORG_ID = "46ac7ad6-7487-4dd0-baa0-6e2c8cae76a0";
    public static final String DEFAULT_PATIENT_MBI = "19990000002901";

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

    public static Endpoint createEndpoint(String organizationId) {
        Endpoint endpoint = new Endpoint();
        endpoint.setName("Test Endpoint");
        endpoint.setAddress("http://www.example.com/endpoint");
        endpoint.setConnectionType(new Coding("http://terminology.hl7.org/CodeSystem/endpoint-connection-type", "hl7-fhir-rest", ""));
        endpoint.setManagingOrganization(new Reference(new IdType("Organization", organizationId)));
        endpoint.setStatus(Endpoint.EndpointStatus.ACTIVE);

        return endpoint;
    }

    public static IGenericClient createFHIRClient(FhirContext ctx, String serverURL) {
        ctx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        return ctx.newRestfulGenericClient(serverURL);
    }
}
