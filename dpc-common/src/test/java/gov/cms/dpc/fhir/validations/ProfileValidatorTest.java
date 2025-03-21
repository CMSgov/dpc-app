package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import gov.cms.dpc.fhir.annotations.Profiled;
import gov.cms.dpc.fhir.validations.profiles.*;
import jakarta.validation.ConstraintValidatorContext;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

class ProfileValidatorTest {

    static FhirContext ctx;
    static DPCProfileSupport dpcModule;
    static FhirValidator fhirValidator;
    static ProfileValidator profileValidator;
    static ConstraintValidatorContext validatorContext = mock(ConstraintValidatorContext.class, RETURNS_DEEP_STUBS);

    @BeforeAll
    static void setup() {
        ctx = FhirContext.forDstu3();

        fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(false);
        fhirValidator.setValidateAgainstStandardSchema(false);
        FhirInstanceValidator instanceValidator = new FhirInstanceValidator(ctx);
        fhirValidator.registerValidatorModule(instanceValidator);

        profileValidator = new ProfileValidator(fhirValidator);
        Profiled profiled = mock(Profiled.class);
        profileValidator.initialize(profiled);

        dpcModule = new DPCProfileSupport(ctx);
        ValidationSupportChain chain = new ValidationSupportChain(
                dpcModule,
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx)
        );
        instanceValidator.setValidationSupport(chain);
    }

    @ParameterizedTest
    @MethodSource("provideResources")
    void testIsValid(BaseResource resource) {
        assertTrue(profileValidator.isValid(resource, validatorContext));
    }

    private static List<BaseResource> provideResources() {
        Organization organization = new Organization();
        organization.setMeta(new Meta().addProfile(OrganizationProfile.PROFILE_URI));
        organization.setId("test-organization");
        organization.setName("Test Organization");
        organization.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-value");

        Address address = new Address();
        address.addLine("123 Sesame St");
        address.setCity("New York");
        address.setState("NY");
        address.setPostalCode("10001");
        address.setCountry("US");
        address.setUse(Address.AddressUse.WORK);
        address.setType(Address.AddressType.PHYSICAL);
        organization.addAddress(address);

        Patient patient = new Patient();
        patient.setMeta(new Meta().addProfile(PatientProfile.PROFILE_URI));
        patient.setId("test-patient");
        patient.setGender(Enumerations.AdministrativeGender.OTHER);
        patient.setManagingOrganization(new Reference("Organization/test-organization"));
        patient.addIdentifier().setSystem(DPCIdentifierSystem.MBI.getSystem()).setValue("test-mbi");
        patient.addName().setFamily("Patient").addGiven("Test");
        patient.setBirthDate(Date.valueOf("1990-01-01"));

        Practitioner practitioner = new Practitioner();
        practitioner.setMeta(new Meta().addProfile(PractitionerProfile.PROFILE_URI));
        practitioner.setId("test-practitioner");
        practitioner.addIdentifier().setSystem(DPCIdentifierSystem.NPPES.getSystem()).setValue("test-npi");
        practitioner.addName().setFamily("Practitioner").addGiven("Test");

        Provenance provenance = new Provenance();
        provenance.setMeta(new Meta().addProfile(AttestationProfile.PROFILE_URI));
        provenance.setRecorded(Date.valueOf("2020-01-01"));
        provenance.addTarget(new Reference("Patient/test"));
        provenance.setReason(List.of(new Coding().setSystem("http://hl7.org/fhir/v3/ActReason").setCode("TREAT")));

        Provenance.ProvenanceAgentComponent agent = new Provenance.ProvenanceAgentComponent();
        agent.setWho(new Reference("Organization/test"));
        agent.setOnBehalfOf(new Reference("Practitioner/test"));

        Coding roleCode = new Coding().setSystem("http://hl7.org/fhir/v3/RoleClass").setCode("AGNT");
        agent.setRole(List.of(new CodeableConcept().addCoding(roleCode)));
        provenance.addAgent(agent);

        return List.of(organization, patient, practitioner, provenance);
    }
}
