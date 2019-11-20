package gov.cms.dpc.fhir.dropwizard.filters.ranges;

import gov.cms.dpc.fhir.dropwizard.filters.ranges.RangedOutputStream;
import gov.cms.dpc.testing.BufferedLoggerHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(BufferedLoggerHandler.class)
class RangedOutputStreamTest {

    /**
     * Tests a single range.
     */
    @Test
    void singleRangeTest() {
        OutputStream baos = new ByteArrayOutputStream();
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        try (RangedOutputStream ros = new RangedOutputStream(baos, "bytes=6-10", "text/plain", headers)) {
            assertEquals("bytes", ros.getAcceptRanges(), "Should have accepted ranges");
            assertFalse(ros.isMultipart(), "Should not be a multi-part response");
            PrintStream printStream = new PrintStream(ros);
            printStream.append("abcdefghijklmnopqrstuvwxyz");
            printStream.flush();
            assertEquals("ghijk", baos.toString());
        } catch (IOException e) {
            fail(e);
        }
    }

    /**
     * Tests multiple ranges.
     */
    @Test
    void multiRangeTest() {
        OutputStream baos = new ByteArrayOutputStream();
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        try (RangedOutputStream ros = new RangedOutputStream(baos, "bytes=6-10,18-", "text/plain", headers)) {
            assertEquals("bytes", ros.getAcceptRanges());
            Assertions.assertTrue(ros.isMultipart());
            String boundary = ros.getBoundary();
            String response =
                    // @formatter:off
                    "--" + boundary + "\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Range: bytes 6-10/26\r\n" +
                            "\r\n" +
                            "ghijk\r\n" +
                            "--" + boundary + "\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Range: bytes 18-26/26\r\n" +
                            "\r\n" +
                            "stuvwxyz\r\n" +
                            "--" + boundary + "--";
            // @formatter:on
            PrintStream printStream = new PrintStream(ros);
            printStream.append("abcdefghijklmnopqrstuvwxyz");
            printStream.flush();
            assertEquals(response, baos.toString());
        } catch (IOException e) {
            fail(e);
        }
    }

}
