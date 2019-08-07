package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.dpc.fhir.validations.profiles.AttributionRosterProfile;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;
import org.hl7.fhir.dstu3.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class RosterValidationTest {

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
    void testAttributed() {
        final Group group = generateFakeGroup();
        final CodeableConcept concept = new CodeableConcept();
        concept.addCoding().setCode("attributed-to");
        group.addCharacteristic().setCode(concept).setExclude(false).setValue(new BooleanType(false));

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
