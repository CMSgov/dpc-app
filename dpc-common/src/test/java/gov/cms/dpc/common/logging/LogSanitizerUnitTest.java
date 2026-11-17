package gov.cms.dpc.common.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static gov.cms.dpc.common.logging.LogSanitizer.sanitize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LogSanitizerUnitTest {

    @Test
    void testNullInputReturnsNull() {
        assertNull(sanitize(null));
    }

    @Test
    void testEmptyStringUnchanged() {
        assertEquals("", sanitize(""));
    }

    @Test
    void testStringWithNoControlCharactersUnchanged() {
        assertEquals("roster-123", sanitize("roster-123"));
    }

    @Test
    void testCarriageReturnReplaced() {
        assertEquals("foo_bar", sanitize("foo\rbar"));
    }

    @Test
    void testLineFeedReplaced() {
        assertEquals("foo_bar", sanitize("foo\nbar"));
    }

    @Test
    void testCrlfReplacedAsTwoCharacters() {
        // \r\n is two distinct characters, each matched and replaced independently,
        // so it becomes two underscores, not one.
        assertEquals("foo__bar", sanitize("foo\r\nbar"));
    }

    @Test
    void testForgedLogEntryIsNeutralized() {
        final String maliciousInput = "roster-123\n2024-01-01 00:00:00 INFO Fake log line injected by attacker";
        final String sanitized = sanitize(maliciousInput);

        assertEquals("roster-123_2024-01-01 00:00:00 INFO Fake log line injected by attacker", sanitized);
    }

    @Test
    void testMultipleControlCharactersAllReplaced() {
        assertEquals("a_b_c_d", sanitize("a\rb\nc\rd"));
    }

    @ParameterizedTest
    @CsvSource({
            "plain-value, plain-value",
            "'value with spaces', 'value with spaces'",
            "'tab\tcharacter', 'tab\tcharacter'"
    })
    void testNonCrlfWhitespaceIsUnaffected(String input, String expected) {
        // Only \r and \n are targeted - other whitespace (spaces, tabs) must pass through
        // untouched, since over-sanitizing would make logs harder to read for no security benefit.
        assertEquals(expected, sanitize(input));
    }
}
