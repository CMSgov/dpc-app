package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.fhir.configuration.DPCFHIRConfiguration;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ValidatorFactory;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class FHIRValidationModuleUnitTest {
	private final DPCFHIRConfiguration.FHIRValidationConfiguration config = mock(DPCFHIRConfiguration.FHIRValidationConfiguration.class);
	private final ConstraintValidatorFactory cvf = mock(ConstraintValidatorFactory.class);
	private final FHIRValidationModule validationModule = new FHIRValidationModule(config);

	@Test
	void createsValidatorFactory() {
		ValidatorFactory factory = validationModule.provideValidatorFactory(cvf);
		assertInstanceOf(ValidatorFactory.class, factory);
	}

	@Test
	void createsValidator() {
		FhirContext ctx = FhirContext.forDstu3();
		ValidationSupportChain chain = new ValidationSupportChain(
			new DefaultProfileValidationSupport(ctx),
			new InMemoryTerminologyServerValidationSupport(ctx));

		FhirValidator validator = validationModule.provideFhirValidator(ctx, chain);
		assertInstanceOf(FhirValidator.class, validator);
	}
}
