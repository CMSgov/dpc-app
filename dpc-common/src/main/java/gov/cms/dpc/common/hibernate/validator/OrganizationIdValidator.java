package gov.cms.dpc.common.hibernate.validator;

import gov.cms.dpc.common.annotations.OrganizationId;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class OrganizationIdValidator implements ConstraintValidator<OrganizationId, String> {

    public static final String VALIDATION_MESSAGE = "Invalid Organization ID format";
    private static final String CARD_ISSUER_IDENTIFIER = "80840";

    @Override
    public void initialize(OrganizationId organizationId) {
        //not used
    }

    @Override
    public boolean isValid(String s, ConstraintValidatorContext constraintValidatorContext) {
        if (!allDigits(s)) {
            return false;
        }
        String identifier = CARD_ISSUER_IDENTIFIER + s;
        return LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(identifier);
    }

    private boolean allDigits(String s) {
        return StringUtils.isNotEmpty(s) && s.chars().allMatch(Character::isDigit);
    }
}
