package gov.cms.dpc.api.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRValidationExceptionHandler;
import gov.cms.dpc.fhir.validations.DPCProfileSupport;
import gov.cms.dpc.fhir.validations.ProfileValidator;
import gov.cms.dpc.fhir.validations.profiles.DefinitionConstants;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidatorProvider;
import gov.cms.dpc.fhir.validations.dropwizard.InjectingConstraintValidatorFactory;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.sql.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DropwizardExtensionsSupport.class)
class TestResourceTest {

    private static final FhirContext ctx = FhirContext.forDstu3();

    private static ResourceExtension RESOURCES = buildResource();

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

        assertEquals(6, operationOutcome.getIssue().size(), "Should have a bunch of issues");
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
        meta.addProfile(DefinitionConstants.DPC_PATIENT_URI.toString());

        patient.setMeta(meta);

        patient.setId("test-patient");
        patient.setGender(Enumerations.AdministrativeGender.MALE);

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

    static Validator provideValidator(InjectingConstraintValidatorFactory factory) {
        return Validation.byDefaultProvider()
                .configure().constraintValidatorFactory(factory)
                .buildValidatorFactory().getValidator();
    }

    private static ResourceExtension buildResource() {
        final DPCFHIRConfiguration.FHIRValidationConfiguration config = new DPCFHIRConfiguration.FHIRValidationConfiguration();
        config.setEnabled(true);
        config.setSchematronValidation(true);
        config.setSchemaValidation(true);
        final InjectingConstraintValidatorFactory constraintFactory = new InjectingConstraintValidatorFactory(
                Set.of(new ProfileValidator(new FHIRValidatorProvider(ctx, new DPCProfileSupport(ctx), config).get())));

        return ResourceExtension
                .builder()
                .setRegisterDefaultExceptionMappers(false)
                .addProvider(new FHIRHandler(ctx))
                .addProvider(FHIRValidationExceptionHandler.class)
                .addProvider(FHIRExceptionHandler.class)
                .addResource(new TestResource())
                .setValidator(provideValidator(constraintFactory))
                .build();
    }
}
