package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.validations.profiles.AttributionRosterProfile;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(BufferedLoggerHandler.class)
public class RosterValidationTest {

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
    void testAttributed() {
        final Group group = generateFakeGroup();
        final CodeableConcept concept = new CodeableConcept();
        concept.addCoding().setCode("attributed-to");
        group.addCharacteristic().setCode(concept).setExclude(false).setValue(new BooleanType(false));

        // Add a single member
        group.addMember().setEntity(new Reference("Patient/yup"));

        final ValidationResult result = fhirValidator.validateWithResult(group);
        assertTrue(result.isSuccessful());
    }


    private Group generateFakeGroup() {
        final Group group = new Group();
        final Meta meta = new Meta();
        meta.addProfile(AttributionRosterProfile.PROFILE_URI);
        group.setMeta(meta);

        group.setActual(true);
        group.setActive(true);
        group.setType(Group.GroupType.PERSON);
        return group;
    }
}
