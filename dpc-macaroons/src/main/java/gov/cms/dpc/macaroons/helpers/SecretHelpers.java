package gov.cms.dpc.macaroons.helpers;

import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;

public class SecretHelpers {

    // Valid chars, which can be encoded as UTF-8
    private static final String VALID_PW_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+{}[]|:;<>?,./";
    private static final int ROOT_KEY_LENGTH = 24;

    private SecretHelpers() {
        // Not used
    }

    /**
     * Securely generate a random password string
     * Borrowing from: https://stackoverflow.com/questions/29756660/create-random-password-using-java-securerandom-class
     *
     * @param random - {@link SecureRandom} to use for password generation
     * @return - {@link String} root key
     */
    public static String generateSecretKey(SecureRandom random) {
        return RandomStringUtils.random(ROOT_KEY_LENGTH, 0, VALID_PW_CHARS.length(), false, false, VALID_PW_CHARS.toCharArray(), random);
    }
}
