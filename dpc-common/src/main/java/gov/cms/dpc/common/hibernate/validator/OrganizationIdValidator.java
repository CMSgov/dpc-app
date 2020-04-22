package gov.cms.dpc.common.hibernate.validator;

import gov.cms.dpc.common.annotations.OrganizationId;
import gov.cms.dpc.common.entities.OrganizationEntity;
import gov.cms.dpc.fhir.DPCIdentifierSystem;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class OrganizationIdValidator implements ConstraintValidator<OrganizationId, OrganizationEntity.OrganizationID> {

    public static final String VALIDATION_MESSAGE = "Invalid Organization ID format";
    private static final String CARD_ISSUER_IDENTIFIER = "80840";
    private static final int IDENTIFIER_LENGTH = 10;

    @Override
    public void initialize(OrganizationId organizationId) {
        //not used
    }

    @Override
    public boolean isValid(OrganizationEntity.OrganizationID id, ConstraintValidatorContext constraintValidatorContext) {
        if (id.getSystem() != DPCIdentifierSystem.NPPES) {
            return true;
        }

        String identifier = id.getValue();
        if (!StringUtils.isNotBlank(identifier) || !allDigits(identifier) || identifier.length() != IDENTIFIER_LENGTH) {
            return false;
        }

        String fullIdentifier = CARD_ISSUER_IDENTIFIER + identifier;
        return LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(fullIdentifier);
    }

    private boolean allDigits(String s) {
        return StringUtils.isNotEmpty(s) && s.chars().allMatch(Character::isDigit);
    }
}
