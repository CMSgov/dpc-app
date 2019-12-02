package gov.cms.dpc.api.converters;

import gov.cms.dpc.api.models.RangeHeader;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static gov.cms.dpc.api.converters.HttpRangeHeaderParamConverter.RANGE_MSG_FORMATTER;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("OptionalGetWithoutIsPresent")
@ExtendWith(BufferedLoggerHandler.class)
class HttpRangeHeaderParamConverterTest {

    private final HttpRangeHeaderParamConverter converter = new HttpRangeHeaderParamConverter();

    HttpRangeHeaderParamConverterTest() {
        // Not used
    }

    @Test
    void testFullParsing() {
        final RangeHeader header = converter.fromString("bytes=0-1");

        assertAll(() -> assertNotNull(header, "Should have header response"),
                () -> assertEquals("bytes", header.getUnit(), "Should have correct unit"),
                () -> assertTrue(header.getStart().isPresent(), "Should have start range"),
                () -> assertEquals(0, header.getStart().get(), "Should have correct start"),
                () -> assertTrue(header.getEnd().isPresent(), "Should have end range"),
                () -> assertEquals(1, header.getEnd().get(), "Should have correct end"));
    }

    @Test
    void testEmptyRequest() {
        final RangeHeader header = converter.fromString("");
        assertNull(header, "Should not have range request");
    }

    @Test
    void testCompletelyBogus() {
        final String bogusRequest = "this is not real, not at all.";
        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> converter.fromString(bogusRequest));
        assertAll(() -> assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals(String.format(RANGE_MSG_FORMATTER, bogusRequest), exception.getMessage(), "Should have correct error message"));
    }

    @Test
    void testMissingEnd() {
        final RangeHeader header = converter.fromString("bytes=0-");

        assertAll(() -> assertNotNull(header, "Should have header response"),
                () -> assertEquals("bytes", header.getUnit(), "Should have correct unit"),
                () -> assertTrue(header.getStart().isPresent(), "Should have start range"),
                () -> assertEquals(0, header.getStart().get(), "Should have correct start"),
                () -> assertFalse(header.getEnd().isPresent(), "Should not have end range"));
    }

    @Test
    void testOnlyStart() {
        final String bogusRequest = "bytes=0";
        assertThrows(WebApplicationException.class, () -> converter.fromString(bogusRequest));

        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> converter.fromString(bogusRequest));
        assertAll(() -> assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals(String.format(RANGE_MSG_FORMATTER, bogusRequest), exception.getMessage(), "Should have correct error message"));
    }

    @Test
    void testSpacing() {
        final String bogusRequest = "bytes = 0";
        final WebApplicationException exception = assertThrows(WebApplicationException.class, () -> converter.fromString(bogusRequest));
        assertAll(() -> assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), exception.getResponse().getStatus(), "Should be a bad request"),
                () -> assertEquals(String.format(RANGE_MSG_FORMATTER, bogusRequest), exception.getMessage(), "Should have correct error message"));
    }
}
