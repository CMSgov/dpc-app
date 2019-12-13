package gov.cms.dpc.api.converters;

import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(BufferedLoggerHandler.class)
class ChecksumParamConverterTest {

    private final ChecksumParamConverter converter = new ChecksumParamConverter();

    ChecksumParamConverterTest() {
        // Not used
    }

    @Test
    void testGzipChecksum() {
        final String checksum = "checksum--gzip";

        assertEquals("checksum", converter.fromString(checksum), "Should have stripped gzip");
    }

    @Test
    void testSimpleChecksum() {
        final String checksum = "checksum";

        assertEquals("checksum", converter.fromString(checksum), "Should directly match");
    }

    @Test
    void testMalformedChecksum() {
        final String checksum = "checksum-deflate";

        assertEquals("checksum-deflate", converter.fromString(checksum), "Should directly match");
    }

    @Test
    void testNullChecksum() {
        assertNull(converter.fromString(null), "Should have null value");
    }
}
