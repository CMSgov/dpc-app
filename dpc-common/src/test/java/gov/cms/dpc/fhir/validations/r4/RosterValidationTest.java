package gov.cms.dpc.fhir.validations.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.validations.profiles.r4.AttributionRosterProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(BufferedLoggerHandler.class)
class RosterValidationTest {

    private static FhirValidator fhirValidator;

    @BeforeAll
    static void setup() {
        FhirContext ctx = FhirContext.forR4();
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator(ctx);

        fhirValidator = ctx.newValidator();
        fhirValidator.registerValidatorModule(instanceValidator);

        DPCProfileSupport dpcModule = new DPCProfileSupport(ctx);
        final ValidationSupportChain chain = new ValidationSupportChain(
                dpcModule,
                new DefaultProfileValidationSupport(ctx),
                new InMemoryTerminologyServerValidationSupport(ctx)
        );
        instanceValidator.setValidationSupport(chain);
    }

    @Test
    void testAttributed() {
        final Group group = generateFakeGroup();
        final CodeableConcept concept = new CodeableConcept();
        concept.addCoding().setCode("attributed-to");
        group.addCharacteristic().setCode(concept).setExclude(false).setValue(new BooleanType(false));
        group.addMember().setEntity(new Reference("Patient/yup"));

        final ValidationResult result = fhirValidator.validateWithResult(group);
        assertTrue(result.isSuccessful(), () -> result.getMessages().toString());
    }

    private Group generateFakeGroup() {
        final Group group = new Group();
        group.setMeta(new Meta().addProfile(AttributionRosterProfile.PROFILE_URI));
        group.setActual(true);
        group.setActive(true);
        group.setType(Group.GroupType.PERSON);
        return group;
    }
}
