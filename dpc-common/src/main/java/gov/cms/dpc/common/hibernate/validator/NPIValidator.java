package gov.cms.dpc.common.hibernate.validator;

import gov.cms.dpc.common.annotations.NPI;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class NPIValidator implements ConstraintValidator<NPI, String> {

    public static final String VALIDATION_MESSAGE = "Invalid NPI format";

    @Override
    public void initialize(NPI npi) {
        //not used
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        return NPIValidationUtil.isValidNPI(s);
    }
}
