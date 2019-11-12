package gov.cms.dpc.api.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.api.APITestHelpers;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.validations.profiles.PatientProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.sql.Date;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(BufferedLoggerHandler.class)
class TestResourceTest {

    private static final FhirContext ctx = FhirContext.forDstu3();

    private static ResourceExtension RESOURCES = APITestHelpers.buildResourceExtension(ctx, List.of(new TestResource()), Collections.emptyList(), true);

    @Test
    void testNonProfiledPatient() {
        final Patient patient = generateFakePatient();
        // Override metadata to remove profile
        patient.setMeta(new Meta());
        final FhirContext ctx = FhirContext.forDstu3();
        final IParser parser = ctx.newJsonParser();
        final String patientString = parser.encodeResourceToString(patient);
        final Response response = RESOURCES.target("/")
                .request(FHIRMediaTypes.FHIR_JSON)
                .post(Entity.entity(patientString, FHIRMediaTypes.FHIR_JSON));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY_422, response.getStatus(), "Should have failed");
    }

    @Test
    void testInvalidPatient() {
        final Patient patient = generateFakePatient();
        final FhirContext ctx = FhirContext.forDstu3();
        final IParser parser = ctx.newJsonParser();
        final String patientString = parser.encodeResourceToString(patient);
        final Response response = RESOURCES.target("/")
                .request(FHIRMediaTypes.FHIR_JSON)
                .post(Entity.entity(patientString, FHIRMediaTypes.FHIR_JSON));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY_422, response.getStatus(), "Should have failed");

        // Get the error messages
        final OperationOutcome operationOutcome = parser.parseResource(OperationOutcome.class, response.readEntity(InputStream.class));

        assertEquals(3, operationOutcome.getIssue().size(), "Should have a couple of issues");
    }

    @Test
    void testValidPatient() {
        final Patient patient = generateFakePatient();
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));
        patient.setMultipleBirth(new BooleanType(false));
        patient.addTelecom().setSystem(ContactPoint.ContactPointSystem.PHONE).setValue("555-555-5501").setUse(ContactPoint.ContactPointUse.MOBILE);
        patient.addAddress(generateFakeAddress());
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mpi");

        final FhirContext ctx = FhirContext.forDstu3();
        final IParser parser = ctx.newJsonParser();
        final String patientString = parser.encodeResourceToString(patient);

        final Response response = RESOURCES.target("/")
                .request(FHIRMediaTypes.FHIR_JSON)
                .post(Entity.entity(patientString, FHIRMediaTypes.FHIR_JSON));

        assertEquals(HttpStatus.OK_200, response.getStatus(), "Should have passed");
    }

    Patient generateFakePatient() {

        final Patient patient = new Patient();
        final Meta meta = new Meta();
        meta.addProfile(PatientProfile.PROFILE_URI);

        patient.setMeta(meta);

        patient.setId("test-patient");
        patient.setGender(Enumerations.AdministrativeGender.MALE);
        patient.setManagingOrganization(new Reference("Organization/test-org"));

        return patient;
    }

    Address generateFakeAddress() {
        final Address address = new Address();
        address.addLine("1800 Pennsylvania Ave NW");
        address.setCity("Washington");
        address.setState("DC");
        address.setPostalCode("20006");
        address.setCountry("US");

        return address;
    }
}
