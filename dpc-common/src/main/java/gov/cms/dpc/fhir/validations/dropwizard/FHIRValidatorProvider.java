package gov.cms.dpc.fhir.validations.dropwizard;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.FhirValidator;
import gov.cms.dpc.fhir.validations.FHIRProfileValidator;
import org.hl7.fhir.dstu3.hapi.ctx.DefaultProfileValidationSupport;
import org.hl7.fhir.dstu3.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.dstu3.hapi.validation.ValidationSupportChain;

import javax.inject.Inject;
import javax.inject.Provider;

public class FHIRValidatorProvider implements Provider<FhirValidator> {

    private final FhirContext ctx;
    private final FHIRProfileValidator dpcModule;

    @Inject
    public FHIRValidatorProvider(FhirContext ctx, FHIRProfileValidator dpcModule) {
        this.ctx = ctx;
        this.dpcModule = dpcModule;
    }


    @Override
    public FhirValidator get() {
        final FhirInstanceValidator instanceValidator = new FhirInstanceValidator();
        final FhirValidator fhirValidator = ctx.newValidator();
        fhirValidator.setValidateAgainstStandardSchematron(false);
        fhirValidator.setValidateAgainstStandardSchema(true);
        fhirValidator.registerValidatorModule(instanceValidator);

        final ValidationSupportChain chain = new ValidationSupportChain(new DefaultProfileValidationSupport(), dpcModule);
        instanceValidator.setValidationSupport(chain);

        return fhirValidator;
    }
}
