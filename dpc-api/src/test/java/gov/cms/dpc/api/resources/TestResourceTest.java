package gov.cms.dpc.api.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.dpc.fhir.FHIRMediaTypes;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRExceptionHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRHandler;
import gov.cms.dpc.fhir.dropwizard.handlers.FHIRValidationExceptionHandler;
import gov.cms.dpc.fhir.validations.definitions.DefinitionConstants;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.jetty.http.HttpStatus;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(DropwizardExtensionsSupport.class)
public class TestResourceTest {

    private static final FhirContext ctx = FhirContext.forDstu3();

    private static final ResourceExtension RESOURCES = ResourceExtension.builder()
            .setRegisterDefaultExceptionMappers(false)
            .addProvider(new FHIRHandler(ctx))
            .addProvider(FHIRValidationExceptionHandler.class)
            .addProvider(FHIRExceptionHandler.class)
            .addResource(new TestResource())
            .build();

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

        assertEquals(1, operationOutcome.getIssue().size(), "Should have a single issue");
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
}
