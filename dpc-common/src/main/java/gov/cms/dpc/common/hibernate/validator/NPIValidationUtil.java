package gov.cms.dpc.common.hibernate.validator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

public final class NPIValidationUtil {

    private static final String CARD_ISSUER_IDENTIFIER = "80840";
    private static final int IDENTIFIER_LENGTH = 10;

    private NPIValidationUtil() {
        //util class
    }

    public static boolean isValidNPI(String npi) {
        if (!StringUtils.isNotBlank(npi) || !allDigits(npi) || npi.length() != IDENTIFIER_LENGTH) {
            return false;
        }

        String fullIdentifier = CARD_ISSUER_IDENTIFIER + npi;
        return LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(fullIdentifier);
    }

    private static boolean allDigits(String s) {
        return StringUtils.isNotEmpty(s) && s.chars().allMatch(Character::isDigit);
    }

}
