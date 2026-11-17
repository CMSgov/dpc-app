package gov.cms.dpc.common.logging;

public final class LogSanitizer {

    private LogSanitizer() {
        // not used
    }

    /**
     * Strips control characters from user input
     */
    public static String sanitize(String input) {
        return input == null ? null : input.replaceAll("[\r\n]", "_");
    }
}
