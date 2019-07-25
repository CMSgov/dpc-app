package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.validations.profiles.EndpointProfile;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.dstu3.model.codesystems.EndpointConnectionType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EndpointValidationTest {

    private static FhirValidator fhirValidator;
    private static DPCProfileSupport dpcModule;
    private static FhirContext ctx;

    @BeforeAll
    static void setup() {
        ctx = FhirContext.forDstu3();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator();

        fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(false);
        fhirValidator.setValidateAgainstStandardSchema(false);
        fhirValidator.registerValidatorModule(instanceValidator);


        dpcModule = new DPCProfileSupport(ctx);
        final ValidationSupportChain chain = new ValidationSupportChain(new DefaultProfileValidationSupport(), dpcModule);
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    void testManagingOrg() {
        final Endpoint endpoint = createFakeEndpoint();
        endpoint.setName("Test Name");
        endpoint.setAddress("http://test.local");

        final ValidationResult result = fhirValidator.validateWithResult(endpoint);
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        // Add a managing org
        endpoint.setManagingOrganization(new Reference("Organization/fake-org"));

        final ValidationResult r2 = fhirValidator.validateWithResult(endpoint);
        assertTrue(r2.isSuccessful());
    }

    @Test
    void testName() {
        final Endpoint endpoint = createFakeEndpoint();
        endpoint.setManagingOrganization(new Reference("Organization/fake-org"));
        endpoint.setAddress("http://test.local");

        final ValidationResult result = fhirValidator.validateWithResult(endpoint);
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        endpoint.setName("Test Name");

        final ValidationResult r2 = fhirValidator.validateWithResult(endpoint);
        assertTrue(r2.isSuccessful());
    }

    @Test
    void testAddress() {
        final Endpoint endpoint = createFakeEndpoint();
        endpoint.setName("Test Name");
        endpoint.setManagingOrganization(new Reference("Organization/fake-org"));

        final ValidationResult result = fhirValidator.validateWithResult(endpoint);
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        endpoint.setAddress("http://test.local");
        final ValidationResult r2 = fhirValidator.validateWithResult(endpoint);
        assertTrue(r2.isSuccessful());
    }

    Endpoint createFakeEndpoint() {
        final Endpoint endpoint = new Endpoint();
        final Meta meta = new Meta();
        meta.addProfile(EndpointProfile.PROFILE_URI);
        endpoint.setMeta(meta);

        // Payload type concept
        final CodeableConcept payloadType = new CodeableConcept();
        payloadType.addCoding().setCode("nothing").setSystem("http://nothing.com");

        endpoint.setPayloadType(List.of(payloadType));

        endpoint.setId("test-endpoint");
        endpoint.setConnectionType(new Coding(EndpointConnectionType.HL7FHIRREST.getSystem(), EndpointConnectionType.HL7FHIRREST.toCode(), EndpointConnectionType.HL7FHIRREST.getDisplay()));
        endpoint.setStatus(Endpoint.EndpointStatus.ACTIVE);
        return endpoint;
    }

}
