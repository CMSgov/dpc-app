package gov.cms.dpc.fhir.validations.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.validations.profiles.r4.OrganizationProfile;
import gov.cms.dpc.fhir.validations.profiles.r4.PractitionerProfile;
import jakarta.validation.ConstraintValidatorContext;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class ProfileValidatorTest {

    private static ProfileValidator validator;
    private static final ConstraintValidatorContext VALIDATION_CONTEXT = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);

    @BeforeAll
    static void setUp() {
        FhirContext context = FhirContext.forR4();
        ValidationSupportChain support = new ValidationSupportChain(
                new DPCProfileSupport(context),
                new DefaultProfileValidationSupport(context),
                new InMemoryTerminologyServerValidationSupport(context));
        FhirValidator fhirValidator = context.newValidator();
        fhirValidator.registerValidatorModule(new FhirInstanceValidator(support));
        validator = new ProfileValidator(fhirValidator);
    }

    @ParameterizedTest
    @MethodSource("provideResources")
    void testIsValid(BaseResource resource) {
        assertTrue(validator.isValid(resource, VALIDATION_CONTEXT));
    }

    private static List<BaseResource> provideResources() {
        Organization organization = new Organization();
        organization.setMeta(new Meta().addProfile(OrganizationProfile.PROFILE_URI));
        organization.setId("test-organization");
        organization.setName("Test Organization");
        organization.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("1111111112");

        Address address = new Address();
        address.addLine("123 Main St");
        address.setCity("Baltimore");
        address.setState("MD");
        address.setPostalCode("21201");
        address.setCountry("US");
        address.setUse(Address.AddressUse.WORK);
        address.setType(Address.AddressType.BOTH);
        organization.addAddress(address);

        Practitioner practitioner = new Practitioner();
        practitioner.setMeta(new Meta().addProfile(PractitionerProfile.PROFILE_URI));
        practitioner.addName().setFamily("Practitioner").addGiven("Test");
        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("1232131239");

        return List.of(organization, practitioner);
    }

    @Test
    void rejectsOrganizationWithoutRequiredIdentifier() {
        Organization organization = new Organization();
        organization.setName("Test Organization");
        organization.addAddress().setUse(org.hl7.fhir.r4.model.Address.AddressUse.WORK)
                .setType(org.hl7.fhir.r4.model.Address.AddressType.BOTH)
                .addLine("123 Main St").setCity("Baltimore").setState("MD").setPostalCode("21201").setCountry("US");

        assertFalse(validator.isValid(organization, VALIDATION_CONTEXT));
    }

    @Test
    void rejectsPractitionerWithoutRequiredIdentifier() {
        Practitioner practitioner = new Practitioner();
        practitioner.addName().setFamily("Practitioner").addGiven("Test");

        assertFalse(validator.isValid(practitioner, VALIDATION_CONTEXT));
    }
}
