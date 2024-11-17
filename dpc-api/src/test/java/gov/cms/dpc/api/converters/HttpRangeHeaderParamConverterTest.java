package gov.cms.dpc.api.converters;

import gov.cms.dpc.api.models.RangeHeader;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static gov.cms.dpc.api.converters.HttpRangeHeaderParamConverter.RANGE_MSG_FORMATTER;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(BufferedLoggerHandler.class)
@DisplayName("HTTP Range header parameter conversion")
class HttpRangeHeaderParamConverterTest {

    private final HttpRangeHeaderParamConverter converter = new HttpRangeHeaderParamConverter();

    HttpRangeHeaderParamConverterTest() {
        // Not used
    }

    @Test
    @DisplayName("Full parse of HTTP range header 🥳")
    void testFullParsing() {
        final String rangeValue = "bytes=0-1";
        final RangeHeader header = converter.fromString(rangeValue);

        assertAll(() -> assertNotNull(header, "Should have header response"),
                () -> assertEquals("bytes", header.getUnit(), "Should have correct unit"),
                () -> assertEquals(0, header.getStart(), "Should have correct start"),
                () -> assertTrue(header.getEnd().isPresent(), "Should have end range"),
                () -> assertEquals(1, header.getEnd().get(), "Should have correct end"));

        assertEquals(rangeValue, converter.toString(header), "Should convert back to string");
    }

    @Test
        @DisplayName("Empty range header 🤮")
    void testEmptyRequest() {
        assertNull(converter.fromString(""), "Should not have range request");
        assertNull(converter.fromString(null), "Should not have range request");
    }

    @Test
    @DisplayName("Malformed range header 🤮")
    void testCompletelyBogus() {
        final String bogusRequest = "this is not real, not at all.";
        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> converter.fromString(bogusRequest));
        assertAll(() -> assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals(String.format(RANGE_MSG_FORMATTER, bogusRequest), exception.getMessage(), "Should have correct error message"));
    }

    @Test
    @DisplayName("Unterminated range header 🤮")
    void testMissingEnd() {
        final String rangeValue = "bytes=0-";
        final RangeHeader header = converter.fromString(rangeValue);

        assertAll(() -> assertNotNull(header, "Should have header response"),
                () -> assertEquals("bytes", header.getUnit(), "Should have correct unit"),
                () -> assertEquals(0, header.getStart(), "Should have correct start"),
                () -> assertFalse(header.getEnd().isPresent(), "Should not have end range"));

        assertEquals(rangeValue, converter.toString(header), "Should convert back to string");
    }

    @Test
    @DisplayName("Non-range in range header 🤮")
    void testOnlyStart() {
        final String bogusRequest = "bytes=0";
        assertThrows(WebApplicationException.class, () -> converter.fromString(bogusRequest));

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> converter.fromString(bogusRequest));
        assertAll(() -> assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals(String.format(RANGE_MSG_FORMATTER, bogusRequest), exception.getMessage(), "Should have correct error message"));
    }

    @Test
        @DisplayName("Range header with whitespace 🤮")
    void testSpacing() {
        final String bogusRequest = "bytes = 0";
        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> converter.fromString(bogusRequest));
        assertAll(() -> assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals(String.format(RANGE_MSG_FORMATTER, bogusRequest), exception.getMessage(), "Should have correct error message"));
    }
}
