package gov.cms.dpc.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NPIUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(NPIUtil.class);
    private static final String CARD_ISSUER_IDENTIFIER = "80840";
    private static final int IDENTIFIER_LENGTH = 10;

    private NPIUtil() {
        //util class
    }

    public static String generateNPI() {
        int randomNumber = (int) Math.floor(Math.random()*(999999999-100000000+1)+100000000);
        String checkDigit = null;
        do {
            try {
                checkDigit = LuhnCheckDigit.LUHN_CHECK_DIGIT.calculate(CARD_ISSUER_IDENTIFIER + randomNumber);
            } catch (Exception ignored) {
                LOGGER.debug("Failed to generate check digit for: {}, trying again", randomNumber);
            }
        } while (checkDigit == null);
        return randomNumber + checkDigit;
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
