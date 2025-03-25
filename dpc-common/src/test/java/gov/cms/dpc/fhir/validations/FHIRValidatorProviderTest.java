package gov.cms.dpc.fhir.validations;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import gov.cms.dpc.fhir.validations.dropwizard.FHIRValidatorProvider;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FHIRValidatorProviderTest {

    @Test
    void getFhirValidator() {
        FhirContext ctx = FhirContext.forDstu3();

        DPCFHIRConfiguration.FHIRValidationConfiguration config = new DPCFHIRConfiguration.FHIRValidationConfiguration();
        config.setEnabled(true);
        config.setSchematronValidation(true);
        config.setSchemaValidation(true);

        ValidationSupportChain supportChain = new ValidationSupportChain(new DPCProfileSupport(ctx));

        FHIRValidatorProvider provider = new FHIRValidatorProvider(ctx, config, supportChain);

        FhirValidator validator = provider.get();
        assertTrue(validator.isValidateAgainstStandardSchema());
    }
}
