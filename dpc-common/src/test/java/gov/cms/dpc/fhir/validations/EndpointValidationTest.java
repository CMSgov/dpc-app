package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationOptions;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.validations.profiles.EndpointProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Reference;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static gov.cms.dpc.testing.factories.OrganizationFactory.createFakeEndpoint;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
    class EndpointValidationTest {

    private static FhirValidator fhirValidator;
    private static DPCProfileSupport dpcModule;
    private static FhirContext ctx;

    @BeforeAll
    static void setup() {
        ctx = FhirContext.forDstu3();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator(ctx);

        fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(false);
        fhirValidator.setValidateAgainstStandardSchema(false);
        fhirValidator.registerValidatorModule(instanceValidator);


        dpcModule = new DPCProfileSupport(ctx);
        final ValidationSupportChain chain = new ValidationSupportChain(
                dpcModule,
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx)
        );
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    void testManagingOrg() {
        final Endpoint endpoint = createFakeEndpoint();
        endpoint.setName("Test Name");
        endpoint.setAddress("http://test.local");

        final ValidationResult result = fhirValidator.validateWithResult(endpoint, new ValidationOptions().addProfile(EndpointProfile.PROFILE_URI));
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        // Add a managing org
        endpoint.setManagingOrganization(new Reference("Organization/fake-org"));

        final ValidationResult r2 = fhirValidator.validateWithResult(endpoint, new ValidationOptions().addProfile(EndpointProfile.PROFILE_URI));
        assertTrue(r2.isSuccessful());
    }

    @Test
    void testName() {
        final Endpoint endpoint = createFakeEndpoint();
        endpoint.setManagingOrganization(new Reference("Organization/fake-org"));
        endpoint.setAddress("http://test.local");

        final ValidationResult result = fhirValidator.validateWithResult(endpoint, new ValidationOptions().addProfile(EndpointProfile.PROFILE_URI));
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

        final ValidationResult result = fhirValidator.validateWithResult(endpoint, new ValidationOptions().addProfile(EndpointProfile.PROFILE_URI));
        assertAll(() -> assertFalse(result.isSuccessful(), "Should have failed validation"),
                () -> assertEquals(1, result.getMessages().size(), "Should have a single failure"));

        endpoint.setAddress("http://test.local");
        final ValidationResult r2 = fhirValidator.validateWithResult(endpoint, new ValidationOptions().addProfile(EndpointProfile.PROFILE_URI));
        assertTrue(r2.isSuccessful());
    }

}
